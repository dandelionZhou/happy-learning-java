package com.happy.learning.zh.auth.security;

import com.happy.learning.zh.auth.service.SmsCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class SmsCodeAuthenticationProvider implements AuthenticationProvider {

    private final SmsCodeService smsCodeService;
    private final UserDetailsService userDetailsService;
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;

    @Autowired
    public SmsCodeAuthenticationProvider(
            SmsCodeService smsCodeService,
            UserDetailsService userDetailsService,
            RegisteredClientRepository registeredClientRepository,
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator) {
        this.smsCodeService = smsCodeService;
        this.userDetailsService = userDetailsService;
        this.registeredClientRepository = registeredClientRepository;
        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        SmsCodeAuthenticationToken smsAuth = (SmsCodeAuthenticationToken) authentication;

        // 1. 验证客户端
        RegisteredClient registeredClient = this.registeredClientRepository.findByClientId(smsAuth.getClientId());
        if (registeredClient == null) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
        }

        // 2. 验证验证码
        if (!smsCodeService.validateCode(smsAuth.getPhone(), smsAuth.getCode())) {
            throw new BadCredentialsException("Invalid verification code");
        }

        // 3. 加载用户
        UserDetails userDetails = userDetailsService.loadUserByUsername(smsAuth.getPhone());
        if (userDetails == null) {
            throw new UsernameNotFoundException("User not found with phone: " + smsAuth.getPhone());
        }

        // 4. 构建认证主体
        Authentication principal = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        // 5. 构建授权上下文
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(userDetails.getUsername())
                .authorizationGrantType(new AuthorizationGrantType("sms_code"))
                .authorizedScopes(registeredClient.getScopes());

        // 6. 生成访问令牌
        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(principal)
                .authorizationGrantType(new AuthorizationGrantType("sms_code"))
                .authorizedScopes(registeredClient.getScopes())
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build();

        // 生成原始令牌
        OAuth2Token generatedToken = this.tokenGenerator.generate(tokenContext);
        if (generatedToken == null || !(generatedToken instanceof Jwt)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.SERVER_ERROR);
        }

        // 转换为 JWT 并构建访问令牌
        Jwt jwt = (Jwt) generatedToken;
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                jwt.getTokenValue(),
                jwt.getIssuedAt(),
                jwt.getExpiresAt(),
                jwt.getClaim(OAuth2ParameterNames.SCOPE)
        );

        // 7. 保存授权信息（调整保存方式）
        // 在保存授权信息时添加刷新令牌
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                UUID.randomUUID().toString(),
                Instant.now(),
                Instant.now().plus(30, ChronoUnit.DAYS)
        );

        OAuth2Authorization authorization = authorizationBuilder
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        this.authorizationService.save(authorization);

        // 8. 返回认证结果
        return new OAuth2AccessTokenAuthenticationToken(
                registeredClient,
                createClientAuthentication(registeredClient),
                accessToken,
                refreshToken
        );
    }

    private OAuth2ClientAuthenticationToken createClientAuthentication(RegisteredClient registeredClient) {
        return new OAuth2ClientAuthenticationToken(
                registeredClient,
                ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                registeredClient.getClientSecret()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return SmsCodeAuthenticationToken.class.isAssignableFrom(authentication);
    }
}