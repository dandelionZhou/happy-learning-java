package com.happy.learning.zh.rbac.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 多级缓存管理器（本地缓存 → Redis）
 */
public class MultiLevelCacheManager implements CacheManager {

    private final Map<String, Cache> caches = new ConcurrentHashMap<>();
    private final com.github.benmanes.caffeine.cache.Cache<Object, Object> localCache;
    private final RedisCacheWriter redisCacheWriter;
    private final RedisCacheConfiguration redisConfig;
    private final Executor asyncCacheExecutor;

    public MultiLevelCacheManager(
            Caffeine<Object, Object> caffeine,
            RedisCacheWriter redisCacheWriter,
            RedisCacheConfiguration redisConfig,
            Executor asyncCacheExecutor
    ) {
        this.localCache = caffeine.build();
        this.redisCacheWriter = redisCacheWriter;
        this.redisConfig = redisConfig;
        this.asyncCacheExecutor = asyncCacheExecutor;
    }


    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, cacheName ->
                new MultiLevelCache(
                        cacheName,
                        localCache,
                        redisCacheWriter,
                        redisConfig,
                        asyncCacheExecutor
                )
        );
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(caches.keySet());
    }

    /**
     * 多级缓存实现（Caffeine + Redis）
     */
    static class MultiLevelCache implements Cache {
        private final String name;
        private final com.github.benmanes.caffeine.cache.Cache<Object, Object> localCache;
        private final RedisCacheWriter redisCacheWriter;
        private final RedisCacheConfiguration redisConfig;
        private final Executor asyncCacheExecutor;
        // 异步加载任务跟踪器（防止重复加载）
        private final ConcurrentMap<Object, CompletableFuture<Object>> loadingMap = new ConcurrentHashMap<>();

        public MultiLevelCache(
                String name,
                com.github.benmanes.caffeine.cache.Cache<Object, Object> localCache,
                RedisCacheWriter redisCacheWriter,
                RedisCacheConfiguration redisConfig,
                Executor asyncCacheExecutor
        ) {
            this.name = name;
            this.localCache = localCache;
            this.redisCacheWriter = redisCacheWriter;
            this.redisConfig = redisConfig;
            this.asyncCacheExecutor = asyncCacheExecutor;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return this;
        }

        @Override
        public ValueWrapper get(Object key) {
            // 1. 先查本地缓存
            Object value = localCache.getIfPresent(key);
            if (value != null) {
                return () -> value;
            }

            // 2. 本地未命中，查 Redis
            byte[] redisKey = serializeKey(key.toString());
            byte[] redisValue = redisCacheWriter.get(name, redisKey);
            if (redisValue != null) {
                Object deserialized = deserializeValue(redisValue);
                // 回填本地缓存
                localCache.put(key, deserialized);
                return () -> deserialized;
            }

            return null;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            ValueWrapper wrapper = get(key);
            if (wrapper == null) {
                return null;
            }
            Object value = wrapper.get();
            if (value != null && type != null && !type.isInstance(value)) {
                throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
            }
            return (T) value;
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            // 1. 先尝试从缓存获取
            T value = get(key, (Class<T>) null);
            if (value != null) {
                return value;
            }

            // 2. 同步加载数据（防止缓存击穿）
            synchronized (key.toString().intern()) {
                // 双重检查锁
                ValueWrapper wrapper = get(key);
                if (wrapper != null) {
                    return (T) wrapper.get();
                }

                // 3. 执行数据加载
                try {
                    value = valueLoader.call();
                } catch (Exception ex) {
                    throw new ValueRetrievalException(key, valueLoader, ex);
                }

                // 4. 写入多级缓存
                put(key, value);
                return value;
            }
        }

        @Override
        public void put(Object key, Object value) {
            // 1. 写入本地缓存
            localCache.put(key, value);

            // 2. 写入 Redis
            byte[] redisKey = serializeKey(key.toString());
            byte[] redisValue = serializeValue(value);
            redisCacheWriter.put(name, redisKey, redisValue, redisConfig.getTtl());
        }

        @Override
        public void evict(Object key) {
            // 删除本地和 Redis 中的缓存
            localCache.invalidate(key);
            byte[] redisKey = serializeKey(key.toString());
            redisCacheWriter.remove(name, redisKey);
        }

        @Override
        public void clear() {
            localCache.invalidateAll();
            redisCacheWriter.clean(name, "*".getBytes());
        }

        // 新增异步获取方法（返回 Future）
        public <T> CompletableFuture<T> getAsync(Object key, Callable<T> valueLoader) {
            return CompletableFuture.supplyAsync(() ->  {
                // 尝试快速路径：同步获取缓存
                T value = get(key, (Class<T>) null);
                if (value != null) {
                    return value;
                }

                // 获取或创建异步加载任务
                CompletableFuture<T> future = (CompletableFuture<T>) loadingMap.computeIfAbsent(key, k ->
                        CompletableFuture.supplyAsync(() -> {
                                    try {
                                        // 执行实际数据加载
                                        T result = valueLoader.call();
                                        // 写入多级缓存（同步操作）
                                        put(key, result);
                                        return result;
                                    } catch (Exception e) {
                                        throw new CompletionException(e);
                                    } finally {
                                        // 移除加载跟踪（无论成功失败）
                                        loadingMap.remove(key);
                                    }
                                }, asyncCacheExecutor)
                                // 异常处理：转换为 Optional 包装
                                .handle((res, ex) -> {
                                    if (ex != null) {
                                        throw new CompletionException(ex.getCause());
                                    }
                                    return res;
                                })
                );

                try {
                    // 阻塞等待结果（可优化为返回 CompletableFuture）
                    return future.join();
                } catch (CompletionException ex) {
                    throw new ValueRetrievalException(key, valueLoader, ex.getCause());
                }

            }, asyncCacheExecutor);
        }

        // 序列化工具方法（使用 Redis 配置的序列化器）
        private byte[] serializeKey(String key) {
            return redisConfig.getKeySerializationPair().write(key).array();
        }

        private byte[] serializeValue(Object value) {
            return redisConfig.getValueSerializationPair().write(value).array();
        }

        private Object deserializeValue(byte[] bytes) {
            return redisConfig.getValueSerializationPair().read(ByteBuffer.wrap(bytes));
        }
    }
}
