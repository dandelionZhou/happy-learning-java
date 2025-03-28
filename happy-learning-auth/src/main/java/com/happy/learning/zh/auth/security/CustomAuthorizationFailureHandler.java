package com.happy.learning.zh.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@Slf4j
public class CustomAuthorizationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        OAuth2Error error = ((OAuth2AuthenticationException) exception).getError();
        log.error("CustomAuthorizationFailureHandler error: {}", error.toString());

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(request.getRequestURI())
                .queryParam("error", error.getErrorCode());

        if (error.getDescription() != null) {
            uriBuilder.queryParam("error_description", error.getDescription());
        }

        response.sendRedirect(uriBuilder.build().toUriString());
    }
}