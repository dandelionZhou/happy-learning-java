package com.happy.learning.zh.auth.config;

import com.happy.learning.zh.auth.security.CustomUserDetails;
import com.happy.learning.zh.auth.service.RedisOAuth2AuthorizationService;
import com.happy.learning.zh.auth.utils.Jwks;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.*;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// AuthorizationServerConfig.java
@Configuration
public class AuthorizationServerConfig {

    @Bean
    public OAuth2AuthorizationService authorizationService(
            RedisOAuth2AuthorizationService redisService) {
        return redisService;
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(@Value("${spring.security.oauth2.authorization-server.issuer}") String issuerUrl) {
        return AuthorizationServerSettings.builder()
                .issuer(issuerUrl)
                .tokenEndpoint("/oauth2/token")
                .build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = Jwks.generateRsa();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri("http://localhost:9000/oauth2/jwks").build();
        /*decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator("http://localhost:9000/oauth2/jwks"),
                new RedisJwtValidator(authService) // 自定义验证器
        ));*/
        return decoder;
    }

    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator(JwtEncoder jwtEncoder, OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer) {
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        jwtGenerator.setJwtCustomizer(tokenCustomizer);
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator, accessTokenGenerator, refreshTokenGenerator
        );
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            // 生成唯一 jti
            String jti = UUID.randomUUID().toString();
            /*context.getClaims().claim("jti", jti);

            // 存储到 Redis（通过 OptimizedRedisOAuth2AuthorizationService）
            redisAuthService.storeJti(jti, context.getPrincipal().getName());*/
            if (context.getTokenType() == OAuth2TokenType.ACCESS_TOKEN) {
                Authentication principal = context.getPrincipal();
                context.getClaims().claim("phone", principal.getName());
            }
            context.getClaims().claim("tenant_id", "123456").claim("uid", "1") // ✅ 添加自定义 Claims
                    .claim("roles", "admin,user").claim("jti", jti); // ✅ 添加角色信息

            Authentication principal = context.getPrincipal();
            if (context.getTokenType().getValue().equals(OAuth2TokenType.ACCESS_TOKEN.getValue())) {
                context.getClaims().claims(claims -> {
                    // 添加角色信息
                    Set<String> roles = principal.getAuthorities().stream()
                            .filter(authority -> authority.getAuthority().startsWith("ROLE_"))
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());
                    claims.put("roles", roles);

                    // 添加细粒度权限
                    Set<String> permissions = principal.getAuthorities().stream()
                            .filter(authority -> !authority.getAuthority().startsWith("ROLE_"))
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());
                    claims.put("permissions", permissions);

                    // 添加自定义用户信息
                    if (principal.getPrincipal() instanceof CustomUserDetails) {
                        CustomUserDetails userDetails = (CustomUserDetails) principal.getPrincipal();
                        claims.put("user_id", userDetails.getUserId());
                        claims.put("user_name", userDetails.getUsername());
                        claims.put("user_email", userDetails.getEmail());
                    }

                });
            }
        };
    }

}