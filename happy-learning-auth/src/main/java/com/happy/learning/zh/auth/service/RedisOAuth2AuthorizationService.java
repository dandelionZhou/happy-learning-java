package com.happy.learning.zh.auth.service;

import com.happy.learning.zh.auth.utils.LuaScriptLoader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
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
    //授权码 → 授权对象
    private static final String AUTHORIZATION_CODE_KEY = "oauth2:authorization_code:";
    //刷新令牌 →
    private static final String REFRESH_TOKEN_KEY = "oauth2:refresh_token:";
    private static final String DEVICE_CODE_KEY = "oauth2:device_code:";
    private static final String USER_CODE_KEY = "oauth2:user_code:";
    //oauth2:authorization:id:{id}	String	授权 ID 到 Token 的映射
    private static final String OAUTH2_AUTHORIZATION_ID_KEY = "oauth2:authorization:id:";
    //oauth2:user_tokens:{user}	Set	用户所有令牌的全局视图
    private static final String USER_TOKEN_MAPPING_KEY = "oauth2:user_tokens:";
    //oauth2:device_tokens:{user}	Hash	用户设备与令牌的映射（device_id → token）
    private static final String DEVICE_TOKEN_MAPPING_KEY = "oauth2:device_tokens:";
    //oauth2:authorization:token_to_id:{token}	String	Token 到授权 ID 的映射
    private static final String TOKEN_TO_ID_KEY = "oauth2:authorization:token_to_id:"; // Token→ID 映射

    //private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringredisTemplate;
    private final DefaultRedisScript<Long> saveScript;
    //private final Jackson2JsonRedisSerializer<Object> jsonSerializer;
    private final RedisTemplate<String, OAuth2Authorization> oauth2AuthorizationRedisTemplate;

    private final int MAX_TOKENS_PER_CLIENT = 2; // 每个客户端允许的最大令牌数

    // 原有构造函数保持不变...
    public RedisOAuth2AuthorizationService(StringRedisTemplate stringredisTemplate, RedisTemplate<String, OAuth2Authorization> oauth2AuthorizationRedisTemplate) {
        this.oauth2AuthorizationRedisTemplate = oauth2AuthorizationRedisTemplate;
        this.stringredisTemplate = stringredisTemplate;
        //this.jsonSerializer = jsonSerializer;
        // 预加载脚本
        this.saveScript = LuaScriptLoader.loadScript("lua/token_management.lua");
       /* // 初始化时配置 ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.addMixIn(OAuth2Authorization.class, OAuth2AuthorizationMixin.class);
        jsonSerializer = new Jackson2JsonRedisSerializer<>(mapper, OAuth2Authorization.class);*/
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");

        /*String clientId = authorization.getRegisteredClientId();
        String userId = authorization.getPrincipalName();
        String accessToken = authorization.getAccessToken().getToken().getTokenValue();
        long expiresIn = getExpireSeconds(authorization);*/
        String grantType = authorization.getAuthorizationGrantType().getValue();
        String authId = authorization.getId();
        String accessToken = getTokenValue(authorization, OAuth2AccessToken.class);
        String refreshToken = getTokenValue(authorization, OAuth2RefreshToken.class);
        String authCode = getTokenValue(authorization, OAuth2AuthorizationCode.class);
        String deviceId = getDeviceId(authorization);
        long accessTokenExpiresIn = getExpireSeconds(authorization, OAuth2AccessToken.class);
        long refreshTokenExpiresIn = getExpireSeconds(authorization, OAuth2RefreshToken.class);
        long authCodeExpiresIn = getExpireSeconds(authorization, OAuth2AuthorizationCode.class);

        // 执行 Lua 脚本
        /*
        oauth2:auth_code:{code}	授权码 → 授权ID
        oauth2:auth_id:{id}	授权ID → 完整授权对象
        oauth2:refresh_token:{token}	刷新令牌 → 访问令牌
        oauth2:token_to_id:{token}	访问令牌 → 授权ID
        oauth2:client_tokens:{client}:{user}	客户端用户令牌集合（ZSet）
        */

        List<String> keys = Arrays.asList(
                "oauth2:client_tokens:" + authorization.getRegisteredClientId() + ":" + authorization.getPrincipalName(),
                "oauth2:user_tokens:" + authorization.getPrincipalName(),
                "oauth2:authorization:",
                "oauth2:device_tokens:" + authorization.getPrincipalName(),
                "oauth2:client_refresh_token:" + authorization.getRegisteredClientId() + ":" + authorization.getPrincipalName()
        );

        // 修正参数传递
        /*List<Object> args = Arrays.asList(
                accessToken,
                deviceId != null ? deviceId : "",
                expiresIn, // 必须转为 String
                new String(jsonSerializer.serialize(authorization)),
                System.currentTimeMillis() / 1000,
                MAX_TOKENS_PER_CLIENT,
                authorization.getId(),
                authorizationCode != null ? authorizationCode : "", // ARGV[8]
                refreshToken != null ? refreshToken : ""  // ARGV[9]
        );*/

        // 构造 Lua 脚本参数
        List<Object> args = Arrays.asList(
                accessToken != null ? accessToken : "",  // ARGV[1]
                deviceId != null ? deviceId : "",        // ARGV[2]
                accessTokenExpiresIn < 0 ? 3600 : accessTokenExpiresIn,               // ARGV[3]
                authorization, // ARGV[4]
                System.currentTimeMillis() / 1000, // ARGV[5]
                MAX_TOKENS_PER_CLIENT,   // ARGV[6]
                authId,                   // ARGV[7]
                authCode != null ? authCode : "", // ARGV[8]
                refreshToken != null ? refreshToken : "",  // ARGV[9]
                grantType, //ARGV[10]
                refreshTokenExpiresIn < 0 ? 3600 : refreshTokenExpiresIn, //ARGV[11]
                authCodeExpiresIn < 0 ? 3600 : authCodeExpiresIn //ARGV[12]
        );

        // 构造参数...
        oauth2AuthorizationRedisTemplate.execute(
                saveScript,
                keys, // List<String>
                args.toArray() // Object[]
        );
    }

    // 修改后的 remove 方法
    @Override
    public void remove(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");
        String authId = authorization.getId();
        String accessToken = getTokenValue(authorization, OAuth2AccessToken.class);
        String refreshToken = getTokenValue(authorization, OAuth2RefreshToken.class);
        String authCode = getTokenValue(authorization, OAuth2AuthorizationCode.class);

        // 清理所有关联键
        stringredisTemplate.delete("oauth2:auth_id:" + authId);
        if (authCode != null) {
            stringredisTemplate.delete("oauth2:auth_code:" + authCode);
            stringredisTemplate.delete("oauth2:auth_id:" + authId + ":code");
        }
        if (accessToken != null) {
            stringredisTemplate.delete("oauth2:authorization:" + accessToken);
            stringredisTemplate.delete("oauth2:token_to_id:" + accessToken);
        }
        if (refreshToken != null) {
            stringredisTemplate.delete("oauth2:refresh_token:" + refreshToken);
        }

    }

    @Override
    public OAuth2Authorization findById(String id) {
        return oauth2AuthorizationRedisTemplate.opsForValue().get("oauth2:auth_id:" + id);
        /*Object data = redisTemplate.opsForValue().get("oauth2:auth_id:" + id);
        return jsonSerializer.deserialize(redisTemplate.opsForValue().get("oauth2:auth_id:" + id).);*/
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        if (tokenType == null) {
            return findByAccessToken(token);
        }
        switch (tokenType.getValue()) {
            case "access_token":
                return findByAccessToken(token);
            case "refresh_token":
                return findByRefreshToken(token);
            case "code":
                return findByAuthCode(token);
            default:
                return null;
        }
    }

    // ========== 辅助方法 ==========
    private OAuth2Authorization findByAccessToken(String token) {
        String authId = stringredisTemplate.opsForValue().get("oauth2:access_token:" + token);
        return oauth2AuthorizationRedisTemplate.opsForValue().get("oauth2:auth_id:" + authId);
    }

    private OAuth2Authorization findByRefreshToken(String token) {
        String authId = stringredisTemplate.opsForValue().get("oauth2:refresh_token:" + token);
        return OAuth2Authorization.from( oauth2AuthorizationRedisTemplate.opsForValue().get("oauth2:auth_id:" + authId))
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN) // 设置新授权类型
                .build();
    }

    private OAuth2Authorization findByAuthCode(String code) {
        String authId = stringredisTemplate.opsForValue().get("oauth2:auth_code:" + code);
        return authId != null ? findById(authId) : null;
    }


    // 查询设备码授权
    public OAuth2Authorization findByDeviceCode(String deviceCode) {
        String accessToken =  stringredisTemplate.opsForValue().get(DEVICE_CODE_KEY + deviceCode);
        return accessToken != null ? findByAccessToken(accessToken) : null;
    }

    public OAuth2Authorization findByUserCode(String userCode) {
        String deviceCode = stringredisTemplate.opsForValue().get(USER_CODE_KEY + userCode);
        return deviceCode != null ? findByDeviceCode(deviceCode) : null;
    }

    private String getTokenValue(OAuth2Authorization authorization, Class<? extends OAuth2Token> tokenClass) {
        OAuth2Authorization.Token<?> token = authorization.getToken(tokenClass);
        return token != null ? token.getToken().getTokenValue() : null;
    }

    // 辅助方法：计算过期时间
    private long getExpireSeconds(OAuth2Authorization authorization, Class<? extends OAuth2Token> tokenClass) {
        /*Instant expiresAt = authorization.getAccessToken().getToken().getExpiresAt();
        return expiresAt != null ?
                Duration.between(Instant.now(), expiresAt).getSeconds() :
                Duration.ofHours(1).getSeconds();*/
        OAuth2Authorization.Token<?> token = authorization.getToken(tokenClass);
        if (token == null) {
            //返回默认1小时
            return 3600;
        }
        Instant expiresAt = token.getToken().getExpiresAt();
        return expiresAt != null ?
                Duration.between(Instant.now(), expiresAt).getSeconds() :
                3600; // 默认 1 小时
    }

    // 设备信息提取（可根据需求扩展）
    private String getDeviceId(OAuth2Authorization authorization) {
        return authorization.getAttribute("device_id");
    }

}