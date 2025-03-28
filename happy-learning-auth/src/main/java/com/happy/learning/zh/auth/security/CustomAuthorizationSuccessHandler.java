package com.happy.learning.zh.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CustomAuthorizationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 获取原始授权码
        OAuth2AuthorizationCodeRequestAuthenticationToken authResult =
                (OAuth2AuthorizationCodeRequestAuthenticationToken) authentication;
        String code = authResult.getAuthorizationCode().getTokenValue();

        // 自定义响应（例如添加额外参数）
        String redirectUri = authResult.getRedirectUri() + "?code=" + code + "&custom_param=123";
        response.sendRedirect(redirectUri);
    }
}
