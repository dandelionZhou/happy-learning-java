package com.happy.learning.zh.auth.security.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;

import java.io.IOException;

public class OAuth2AuthorizationSerializer extends JsonSerializer<OAuth2Authorization> {

    @Override
    public void serialize(OAuth2Authorization authorization, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        // 只序列化原始对象，不再创建新对象
        gen.writeStartObject();
        gen.writeStringField("id", authorization.getId());
        gen.writeStringField("registeredClientId", authorization.getRegisteredClientId());
        gen.writeStringField("principalName", authorization.getPrincipalName());
        gen.writeStringField("authorizationGrantType", authorization.getAuthorizationGrantType().getValue());
        gen.writeObjectField("authorizedScopes", authorization.getAuthorizedScopes());
        gen.writeObjectField("attributes", authorization.getAttributes());
        // 序列化 accessToken
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getToken(OAuth2AccessToken.class);
        if (accessToken != null) {
            writeToken(gen, "access_token", accessToken);
        }

        // 序列化 refreshToken
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getToken(OAuth2RefreshToken.class);
        if (refreshToken != null) {
            writeToken(gen, "refresh_token", refreshToken);
        }

        // 序列化 authorizationCode
        OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode = authorization.getToken(OAuth2AuthorizationCode.class);
        if (authorizationCode != null) {
            writeToken(gen, "authorization_code", authorizationCode);
        }

        gen.writeEndObject();
    }

    private void writeToken(JsonGenerator gen, String tokenName, OAuth2Authorization.Token<?> token) throws IOException {
        gen.writeObjectFieldStart(tokenName);
        gen.writeStringField("tokenValue", token.getToken().getTokenValue());
        gen.writeObjectField("issuedAt", token.getToken().getIssuedAt());
        gen.writeObjectField("expiresAt", token.getToken().getExpiresAt());
        gen.writeObjectField("metadata", token.getMetadata()); // 包含额外的 token 信息
        gen.writeEndObject();
    }

}

