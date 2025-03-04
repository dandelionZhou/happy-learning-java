package com.happy.learning.zh.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "bl:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void revokeToken(Jwt jwt) {
        String jti = jwt.getId();
        Duration ttl = Duration.between(Instant.now(), jwt.getExpiresAt());
        redisTemplate.opsForValue().set(
                BLACKLIST_KEY_PREFIX + jti,
                "revoked",
                ttl
        );
    }

    public boolean isRevoked(String jti) {
        return redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti);
    }
}
