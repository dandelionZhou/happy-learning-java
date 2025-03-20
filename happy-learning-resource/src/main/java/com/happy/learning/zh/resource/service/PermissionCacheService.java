package com.happy.learning.zh.resource.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.happy.learning.zh.resource.feign.RbacClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *  权限缓存服务
 */

@Service
public class PermissionCacheService {
    // 本地缓存（Caffeine）
    private final Cache<String, Set<String>> localCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RbacClient rbacClient;

    /**
     * 获取权限（多级缓存策略）
     * @param userId 用户 ID
     * @param roles 用户角色列表
     * @return 权限集合
     */
    public Set<String> getPermissions(String userId, List<String> roles) {
        String cacheKey = "permissions:" + userId;

        // 1. 检查本地缓存
        Set<String> permissions = localCache.getIfPresent(cacheKey);
        if (permissions != null) return permissions;

        // 2. 检查 Redis 缓存
        permissions = (Set<String>) redisTemplate.opsForValue().get(cacheKey);
        if (permissions != null) {
            localCache.put(cacheKey, permissions);
            return permissions;
        }

        // 3. 调用 RBAC 服务（防缓存击穿锁）
        synchronized (cacheKey.intern()) {
            // 双重检查锁
            permissions = (Set<String>) redisTemplate.opsForValue().get(cacheKey);
            if (permissions != null) {
                localCache.put(cacheKey, permissions);
                return permissions;
            }

            // 实际查询 RBAC 服务
            permissions = rbacClient.getPermissions(roles);

            // 写入缓存
            redisTemplate.opsForValue().set(cacheKey, permissions, 10, TimeUnit.MINUTES);
            localCache.put(cacheKey, permissions);

            return permissions;
        }
    }

    /**
     * 清除权限缓存
     * @param userId 用户 ID
     */
    public void evictCache(String userId) {
        String cacheKey = "permissions:" + userId;
        localCache.invalidate(cacheKey);
        redisTemplate.delete(cacheKey);
    }
}
