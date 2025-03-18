package com.happy.learning.zh.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Base64;

@Component
public class SmsCodeAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!"sms_code".equals(grantType)) {
            return null;
        }

        String phone = request.getParameter("phone");
        String code = request.getParameter("code");
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Basic ")) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        // 移除 "Basic " 前缀
        String base64Credentials = authorization.substring(6).trim();

        // Base64 解码
        byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
        String credentials = new String(decodedBytes);
        // 拆分 client_id 和 client_secret
        String[] parts = credentials.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid client credentials format");
        }

        if (!StringUtils.hasText(phone) || !StringUtils.hasText(code)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        return new SmsCodeAuthenticationToken(phone, code, parts[0]);
    }
}
