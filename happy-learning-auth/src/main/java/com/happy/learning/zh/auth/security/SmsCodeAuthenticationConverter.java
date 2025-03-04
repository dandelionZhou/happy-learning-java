package com.happy.learning.zh.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
        String clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID);

        if (!StringUtils.hasText(phone) || !StringUtils.hasText(code)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        return new SmsCodeAuthenticationToken(phone, code, clientId);
    }
}
