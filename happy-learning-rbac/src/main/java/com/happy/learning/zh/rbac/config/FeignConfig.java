package com.happy.learning.zh.rbac.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor oauth2FeignRequestInterceptor() {
        return requestTemplate -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                // 从 JwtAuthenticationToken 中提取令牌
                if (authentication instanceof JwtAuthenticationToken) {
                    Jwt jwt = (Jwt) authentication.getPrincipal();
                    requestTemplate.header("Authorization", "Bearer " + jwt.getTokenValue());
                }
                // 其他认证类型处理（如 OAuth2Login）
                else if (authentication.getPrincipal() instanceof OAuth2User) {
                    OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                    String token = oauth2User.getAttribute("access_token");
                    requestTemplate.header("Authorization", "Bearer " + token);
                }
            }
        };
    }
}
