package com.happy.learning.zh.auth.security.serialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class OAuth2AuthorizationDeserializer extends JsonDeserializer<OAuth2Authorization> {

    private final RegisteredClientRepository registeredClientRepository;

    public OAuth2AuthorizationDeserializer(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    public OAuth2Authorization deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = p.getCodec();
        Map<String, Object> node = codec.readValue(p, Map.class);

        // 先获取 registeredClientId
        String registeredClientId = (String) node.get("registeredClientId");
        RegisteredClient registeredClient = registeredClientRepository.findById(registeredClientId);
        if (registeredClient == null) {
            throw new IllegalArgumentException("RegisteredClient not found for id: " + registeredClientId);
        }

        // 解决 authorizedScopes 类型问题
        List<String> scopesList = (List<String>) node.get("authorizedScopes");
        Set<String> authorizedScopes = new HashSet<>(scopesList); // 转换为 HashSet

        // 处理 attributes 里的 OAuth2AuthorizationRequest
        Map<String, Object> attributesMap = (Map<String, Object>) node.get("attributes");

        if (attributesMap.containsKey("org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest")) {

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.registerModule(new JavaTimeModule());  // 处理时间格式
            objectMapper.registerModule(new SimpleModule().addDeserializer(OAuth2AuthorizationResponseType.class,
                    new JsonDeserializer<OAuth2AuthorizationResponseType>() {
                        @Override
                        public OAuth2AuthorizationResponseType deserialize(JsonParser p, DeserializationContext ctxt)
                                throws IOException { // 解析 responseType 对象中的 "value" 字段
                            JsonNode node = p.getCodec().readTree(p);
                            String value = node.get("value").asText();  // 获取 "value" 字段
                            return new OAuth2AuthorizationResponseType(value);
                        }
                    }));


            Object rawAuthRequest = attributesMap.get("org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest");

            if (rawAuthRequest instanceof Map) {
                Map<String, Object> authRequestMap = (Map<String, Object>) rawAuthRequest;
                OAuth2AuthorizationRequest authRequest = objectMapper.convertValue(rawAuthRequest, OAuth2AuthorizationRequest.class);
                attributesMap.put("org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest", authRequest);
            }
        }

        if (attributesMap.containsKey("java.security.Principal")) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            Object rawPrincipal = attributesMap.get("java.security.Principal");

            if (rawPrincipal instanceof Map) {
                Map<String, Object> principalMap = (Map<String, Object>) rawPrincipal;

                // 获取 principalName
                String principalName = (String) principalMap.get("name");

                // 解析 authorities
                List<Map<String, String>> authorityList = (List<Map<String, String>>) principalMap.get("authorities");
                List<GrantedAuthority> authorities = authorityList.stream()
                        .map(authMap -> new SimpleGrantedAuthority(authMap.get("authority")))
                        .collect(Collectors.toList());

                // 构造 UsernamePasswordAuthenticationToken
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principalName, null, authorities);

                // 替换 attributesMap 中的 java.security.Principal
                attributesMap.put("java.security.Principal", authentication);
            }
        }

        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName((String) node.get("principalName"))
                .authorizationGrantType(new AuthorizationGrantType((String) node.get("authorizationGrantType")))
                .authorizedScopes(authorizedScopes)
                .attributes(attrs -> attrs.putAll(attributesMap));

        Object authorizationCode = node.get("authorization_code");
        OAuth2AuthorizationCode oAuth2AuthorizationCode = getAuthCodeToken(authorizationCode);
        Map<String, Object> metadata = getTokenMetadata(authorizationCode);
        if (oAuth2AuthorizationCode != null && !CollectionUtils.isEmpty(metadata)) {
            builder.token(oAuth2AuthorizationCode, metadataMap -> metadataMap.putAll(metadata));
        }

        Object accessToken = node.get("access_token");
        OAuth2AccessToken oAuth2AccessToken = getAccessToken(accessToken);
        if (oAuth2AccessToken != null) {
            builder.accessToken(oAuth2AccessToken);
        }

        Object refreshToken = node.get("refresh_token");
        OAuth2RefreshToken oAuth2RefreshToken = getRefreshToken(refreshToken);
        if (oAuth2RefreshToken != null) {
            builder.refreshToken(oAuth2RefreshToken);
        }

        return builder.build();
    }

    private OAuth2AuthorizationCode getAuthCodeToken(Object objectMap) {
        if (objectMap instanceof Map) {
            Map<String, Object> authorizationCodeMap = (Map<String, Object>) objectMap;
            String tokenValue = authorizationCodeMap.get("tokenValue").toString();
            Instant issuedAt = Instant.parse(authorizationCodeMap.get("issuedAt").toString());
            Instant expiresAt = Instant.parse(authorizationCodeMap.get("expiresAt").toString());
            // 解析 metadata
            return new OAuth2AuthorizationCode(tokenValue, issuedAt, expiresAt);
        }
        return null;
    }

    private OAuth2AccessToken getAccessToken(Object objectMap) {
        if (objectMap instanceof Map) {
            Map<String, Object> authorizationCodeMap = (Map<String, Object>) objectMap;
            String tokenValue = authorizationCodeMap.get("tokenValue").toString();
            Instant issuedAt = Instant.parse(authorizationCodeMap.get("issuedAt").toString());
            Instant expiresAt = Instant.parse(authorizationCodeMap.get("expiresAt").toString());
            // 从 metadata 中提取 scope（注意类型转换）
            Map<String, Object> metadata = (Map<String, Object>) authorizationCodeMap.get("metadata");
            Map<String, Object> claims = (Map<String, Object>) metadata.get("metadata.token.claims");
            Set<String> scopes = new LinkedHashSet<>((Collection<? extends String>) claims.get("scope"));
            // 解析 metadata
            return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, tokenValue,
                    issuedAt, expiresAt, scopes);
        }
        return null;
    }

    private OAuth2RefreshToken getRefreshToken(Object objectMap) {
        if (objectMap instanceof Map) {
            Map<String, Object> authorizationCodeMap = (Map<String, Object>) objectMap;
            String tokenValue = authorizationCodeMap.get("tokenValue").toString();
            Instant issuedAt = Instant.parse(authorizationCodeMap.get("issuedAt").toString());
            Instant expiresAt = Instant.parse(authorizationCodeMap.get("expiresAt").toString());
            // 解析 metadata
            return new OAuth2RefreshToken(tokenValue, issuedAt, expiresAt);
        }
        return null;
    }

    private Map<String, Object> getTokenMetadata(Object objectMap) {
        if (objectMap instanceof Map) {
            Map<String, Object> authorizationCodeMap = (Map<String, Object>) objectMap;

            return (Map<String, Object>) authorizationCodeMap.get("metadata");
        }
        return null;
    }
}

