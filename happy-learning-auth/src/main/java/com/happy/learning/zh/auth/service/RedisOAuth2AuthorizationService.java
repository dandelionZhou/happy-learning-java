package com.happy.learning.zh.auth.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.happy.learning.zh.auth.utils.LuaScriptLoader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

    // 新增客户端令牌限制相关 Key
    //oauth2:client_user_tokens:{client}:{user}	ZSet	客户端-用户维度的令牌集合（按过期时间排序）
    private static final String CLIENT_USER_TOKENS_KEY = "oauth2:client_user_tokens:";
    //oauth2:authorization:{token}	String	完整的授权对象（JSON 格式）
    private static final String OAUTH2_AUTHORIZATION_KEY = "oauth2:authorization:";
    //oauth2:authorization:id:{id}	String	授权 ID 到 Token 的映射
    private static final String OAUTH2_AUTHORIZATION_ID_KEY = "oauth2:authorization:id:";
    //oauth2:user_tokens:{user}	Set	用户所有令牌的全局视图
    private static final String USER_TOKEN_MAPPING_KEY = "oauth2:user_tokens:";
    //oauth2:device_tokens:{user}	Hash	用户设备与令牌的映射（device_id → token）
    private static final String DEVICE_TOKEN_MAPPING_KEY = "oauth2:device_tokens:";
    //oauth2:authorization:token_to_id:{token}	String	Token 到授权 ID 的映射
    private static final String TOKEN_TO_ID_KEY = "oauth2:authorization:token_to_id:"; // Token→ID 映射

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> saveScript;
    private final Jackson2JsonRedisSerializer<OAuth2Authorization> jsonSerializer;

    private final int MAX_TOKENS_PER_CLIENT = 2; // 每个客户端允许的最大令牌数

    // 原有构造函数保持不变...
    public RedisOAuth2AuthorizationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        // 预加载脚本
        this.saveScript = LuaScriptLoader.loadScript("lua/token_management.lua");
        // 初始化时配置 ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.addMixIn(OAuth2Authorization.class, OAuth2AuthorizationMixin.class);
        jsonSerializer = new Jackson2JsonRedisSerializer<>(mapper, OAuth2Authorization.class);
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");

        String clientId = authorization.getRegisteredClientId();
        String userId = authorization.getPrincipalName();
        String accessToken = authorization.getAccessToken().getToken().getTokenValue();
        long expiresIn = getExpireSeconds(authorization);


        // 执行 Lua 脚本
        List<String> keys = Arrays.asList(
                CLIENT_USER_TOKENS_KEY + clientId + ":" + userId,  // 客户端-用户组合 Key
                USER_TOKEN_MAPPING_KEY + userId,
                OAUTH2_AUTHORIZATION_KEY,
                DEVICE_TOKEN_MAPPING_KEY + userId
        );

        // 修正参数传递
        List<Object> args = Arrays.asList(
                accessToken,
                getDeviceId(authorization) != null ? getDeviceId(authorization) : "",
                expiresIn, // 必须转为 String
                new String(jsonSerializer.serialize(authorization)),
                System.currentTimeMillis() / 1000,
                MAX_TOKENS_PER_CLIENT,
                authorization.getId()
        );

        // 构造参数...
        redisTemplate.execute(
                saveScript,
                keys, // List<String>
                args.toArray() // Object[]
        );
    }

    // 修改后的 remove 方法
    @Override
    public void remove(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");
        String token = authorization.getAccessToken().getToken().getTokenValue();
        String clientId = authorization.getRegisteredClientId();
        String userId = authorization.getPrincipalName();
        String authorizationId = authorization.getId();

        // 删除核心数据
        redisTemplate.delete(OAUTH2_AUTHORIZATION_KEY + token); // 主记录
        redisTemplate.delete(OAUTH2_AUTHORIZATION_ID_KEY + authorizationId);      // ID→Token
        redisTemplate.delete(TOKEN_TO_ID_KEY + token);                // Token→ID

        // 从客户端用户集合移除
        redisTemplate.opsForZSet().remove(
                CLIENT_USER_TOKENS_KEY + clientId + ":" + userId,
                token
        );

        // 从用户全局集合移除
        redisTemplate.opsForSet().remove(
                USER_TOKEN_MAPPING_KEY + userId,
                token
        );

        // 清理设备映射
        String deviceId = getDeviceId(authorization);
        if (deviceId != null) {
            redisTemplate.opsForHash().delete(
                    DEVICE_TOKEN_MAPPING_KEY + userId,
                    deviceId
            );
        }

    }

    @Override
    public OAuth2Authorization findById(String id) {
        // 1. 通过 ID 查找 Token
        String token = (String) redisTemplate.opsForValue().get(OAUTH2_AUTHORIZATION_ID_KEY + id);
        if (token == null) return null;

        // 2. 通过 Token 获取完整记录
        return findByAccessToken(token);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        if (tokenType == null) {
            return findByAccessToken(token); // 默认查 access_token
        }
        return switch (tokenType.getValue()) {
            case "access_token" -> findByAccessToken(token);
            case "refresh_token" -> findByRefreshToken(token);
            default -> null;
        };
    }

    // ========== 辅助方法 ==========
    private OAuth2Authorization findByAccessToken(String token) {
        return deserializeAuthorization(
                (byte[]) redisTemplate.opsForValue().get(OAUTH2_AUTHORIZATION_KEY + token)
        );
    }

    private OAuth2Authorization deserializeAuthorization(byte[] data) {
        return data != null ? jsonSerializer.deserialize(data) : null;
    }

    private OAuth2Authorization findByRefreshToken(String refreshToken) {
        // 需要维护 refresh_token 映射（示例实现）
        String token = (String) redisTemplate.opsForValue().get("oauth2:refresh_tokens:" + refreshToken);
        return token != null ? findByAccessToken(token) : null;
    }

    private byte[] serializeAuthorization(OAuth2Authorization authorization) {
        return jsonSerializer.serialize(authorization);
    }

    // 辅助方法：计算过期时间
    private long getExpireSeconds(OAuth2Authorization authorization) {
        Instant expiresAt = authorization.getAccessToken().getToken().getExpiresAt();
        return expiresAt != null ?
                Duration.between(Instant.now(), expiresAt).getSeconds() :
                Duration.ofHours(1).getSeconds();
    }

    // 设备信息提取（可根据需求扩展）
    private String getDeviceId(OAuth2Authorization authorization) {
        return authorization.getAttribute("device_id");
    }

    // ========== Mixin 解决序列化问题 ==========
    @JsonIgnoreProperties({
            "accessToken.token",
            "refreshToken.token",
            "authorizedScopes",
            "attributes"
    })
    private abstract static class OAuth2AuthorizationMixin {}
}