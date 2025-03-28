package com.happy.learning.zh.auth.security.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthorizationRedisSerializer {

    private final ObjectMapper objectMapper;

    public OAuth2AuthorizationRedisSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RedisSerializer<OAuth2Authorization> getSerializer() {
        Jackson2JsonRedisSerializer<OAuth2Authorization> serializer = new Jackson2JsonRedisSerializer<>(OAuth2Authorization.class);
        serializer.setObjectMapper(objectMapper);
        return serializer;
    }
}

