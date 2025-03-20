package com.happy.learning.zh.rbac.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.concurrent.*;

// CacheConfig.java
@Configuration
@EnableCaching
public class CacheConfig {

    // 本地缓存（Caffeine）
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000);
    }

    @Bean
    public Executor asyncCacheExecutor() {
        return new ThreadPoolExecutor(
                4, 16, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                Executors.defaultThreadFactory()
        );
    }

    // 多级缓存管理器（Caffeine → Redis）
    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory redisFactory,
            Caffeine<Object, Object> caffeine,
            Executor asyncCacheExecutor
    ) {
        RedisCacheWriter redisWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisFactory);
        RedisCacheConfiguration redisConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));
        return new MultiLevelCacheManager(
                caffeine,
                redisWriter,
                redisConfig,
                asyncCacheExecutor
        );
    }

    private class CustomThreadFactory {
        public CustomThreadFactory(String s) {
        }
    }
}