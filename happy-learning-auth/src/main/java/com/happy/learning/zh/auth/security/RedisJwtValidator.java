package com.happy.learning.zh.auth.security;

import com.happy.learning.zh.auth.service.RedisOAuth2AuthorizationService;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class RedisJwtValidator implements OAuth2TokenValidator<Jwt> {

    private final RedisOAuth2AuthorizationService redisAuthService;

    public RedisJwtValidator(RedisOAuth2AuthorizationService authService) {
        this.redisAuthService = authService;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        /*String jti = jwt.getId();
        if (!redisAuthService.isJtiActive(jti)) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        }*/
        return OAuth2TokenValidatorResult.success();
    }
}
