package com.happy.learning.zh.auth.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.happy.learning.zh.auth.entity.OAuth2Client;
import com.happy.learning.zh.auth.mapper.OAuth2ClientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.ConfigurationSettingNames;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// JdbcRegisteredClientRepository.java
@Repository
@RequiredArgsConstructor
public class JdbcRegisteredClientRepository implements RegisteredClientRepository {

    private final OAuth2ClientMapper clientMapper;

    @Override
    public void save(RegisteredClient registeredClient) {

    }

    @Override
    public RegisteredClient findById(String id) {
        // 根据 ID 查询实现
        OAuth2Client auth2Client = clientMapper.findByClientId(id);
        return convertToRegisteredClient(auth2Client);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        OAuth2Client client = clientMapper.findByClientId(clientId);
        return convertToRegisteredClient(client);
    }

    private RegisteredClient convertToRegisteredClient(OAuth2Client client) {
        if (!"ENABLED".equals(client.getStatus())) {
            throw new RuntimeException("客户端已禁用");
        }

        return RegisteredClient.withId(client.getId())
                .clientId(client.getClientId())
                // 重要：直接使用存储的加密密码，不再重复加密
                .clientSecret(client.getClientSecret())
                .clientAuthenticationMethods(methods ->
                        methods.addAll(parseSet(client.getClientAuthenticationMethods(),
                                ClientAuthenticationMethod::new)))
                .authorizationGrantTypes(types ->
                        types.addAll(parseSet(client.getAuthorizationGrantTypes(),
                                AuthorizationGrantType::new)))
                .redirectUris(uris ->
                        uris.addAll(parseStringList(client.getRedirectUris())))
                .scopes(scopes ->
                        scopes.addAll(parseStringList(client.getScopes())))
                .clientSettings(parseClientSettings(client.getClientSettings()))
                .tokenSettings(parseTokenSettings(client.getTokenSettings()))
                .build();
    }

    // 新增通用解析方法
    private <T> Set<T> parseSet(String json, Function<String, T> converter) {
        return parseStringList(json).stream()
                .map(converter)
                .collect(Collectors.toSet());
    }

    private List<String> parseStringList(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 解析失败", e);
        }
    }

    private ClientSettings parseClientSettings(String json) {
        try {
            Map<String, Object> settings = new ObjectMapper().readValue(json,
                    new TypeReference<Map<String, Object>>() {});
            return ClientSettings.withSettings(settings).build();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("客户端设置解析失败", e);
        }
    }

    private TokenSettings parseTokenSettings(String json) {
        try {
            Map<String, Object> settings = new ObjectMapper().readValue(json,
                    new TypeReference<Map<String, Object>>() {});

            // 处理 accessTokenFormat 的字符串值
            if (settings.containsKey(ConfigurationSettingNames.Token.ACCESS_TOKEN_FORMAT)) {
                String accessTokenFormat = (String) settings.get(ConfigurationSettingNames.Token.ACCESS_TOKEN_FORMAT);
                if ("REFERENCE".equals(accessTokenFormat)) {
                    settings.put(ConfigurationSettingNames.Token.ACCESS_TOKEN_FORMAT, OAuth2TokenFormat.REFERENCE);
                } else {
                    // 设置默认值
                    settings.put(ConfigurationSettingNames.Token.ACCESS_TOKEN_FORMAT, OAuth2TokenFormat.SELF_CONTAINED);
                }
            } else {
                // 设置默认值
                settings.put(ConfigurationSettingNames.Token.ACCESS_TOKEN_FORMAT, OAuth2TokenFormat.SELF_CONTAINED);
            }

            if (settings.containsKey(ConfigurationSettingNames.Token.ACCESS_TOKEN_TIME_TO_LIVE)) {
                Long tokenExpire = Long.parseLong(settings.get(ConfigurationSettingNames.Token.ACCESS_TOKEN_TIME_TO_LIVE).toString());
                settings.put(ConfigurationSettingNames.Token.ACCESS_TOKEN_TIME_TO_LIVE, Duration.ofDays(tokenExpire));
            } else {
                settings.put(ConfigurationSettingNames.Token.ACCESS_TOKEN_TIME_TO_LIVE, Duration.ofDays(7));
            }

            return TokenSettings.withSettings(settings).build();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("客户端设置解析失败", e);
        }
    }

    private Set<ClientAuthenticationMethod> convertAuthenticationMethods(Collection<String> methods) {
        return methods.stream()
                .map(ClientAuthenticationMethod::new)
                .collect(Collectors.toSet());
    }

    private Set<AuthorizationGrantType> convertGrantTypes(Collection<String> types) {
        return types.stream()
                .map(AuthorizationGrantType::new)
                .collect(Collectors.toSet());
    }
}
