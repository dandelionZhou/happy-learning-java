package com.happy.learning.zh.auth.filter;

import com.happy.learning.zh.auth.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtRevocationCheckFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final TokenBlacklistService blacklistService;

    public JwtRevocationCheckFilter(JwtDecoder jwtDecoder,
                                    TokenBlacklistService blacklistService) {
        this.jwtDecoder = jwtDecoder;
        this.blacklistService = blacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null) {
            try {
                Jwt jwt = jwtDecoder.decode(token);
                if (blacklistService.isRevoked(jwt.getId())) {
                    throw new JwtException("Token revoked");
                }
            } catch (JwtException e) {
                handleAuthenticationFailure(response, e.getMessage());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void handleAuthenticationFailure(HttpServletResponse response,
                                             String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write(message);
        response.getWriter().flush();
    }
}