package com.happy.learning.zh.auth.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class CustomAuthorizationCodeGenerator implements OAuth2TokenGenerator<OAuth2AuthorizationCode> {

    @Override
    public OAuth2AuthorizationCode generate(OAuth2TokenContext context) {
        // 仅处理授权码生成请求
        if (!AuthorizationGrantType.AUTHORIZATION_CODE.equals(context.getTokenType())) {
            return null; // 其他类型的 Token 交给默认生成器处理
        }

        // 自定义授权码逻辑（例如：添加前缀）
        String codeValue = "CUSTOM_" + UUID.randomUUID().toString().replace("-", "");
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofMinutes(5)); // 5分钟有效期
        log.debug("自定义授权码生成：{}", codeValue);
        return new OAuth2AuthorizationCode(codeValue, issuedAt, expiresAt);
    }
}
