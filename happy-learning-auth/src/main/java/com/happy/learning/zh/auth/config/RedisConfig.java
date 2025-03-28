package com.happy.learning.zh.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.happy.learning.zh.auth.security.serialize.OAuth2AuthorizationDeserializer;
import com.happy.learning.zh.auth.security.serialize.OAuth2AuthorizationRedisSerializer;
import com.happy.learning.zh.auth.security.serialize.OAuth2AuthorizationSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, OAuth2Authorization> oauth2AuthorizationRedisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            OAuth2AuthorizationRedisSerializer serializer) {

        RedisTemplate<String, OAuth2Authorization> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(template.getStringSerializer());
        template.setValueSerializer(serializer.getSerializer());
        template.afterPropertiesSet();
        return template;
    }


    @Bean
    public ObjectMapper objectMapper(RegisteredClientRepository registeredClientRepository) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        SimpleModule module = new SimpleModule();
        module.addSerializer(OAuth2Authorization.class, new OAuth2AuthorizationSerializer());
        module.addDeserializer(OAuth2Authorization.class, new OAuth2AuthorizationDeserializer(registeredClientRepository));
        objectMapper.registerModule(module);
        return objectMapper;
    }


    @Bean
    public StringRedisTemplate stringredisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }


}
