package com.happy.learning.zh.auth.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

// SmsCodeAuthenticationToken.java
public class SmsCodeAuthenticationToken extends AbstractAuthenticationToken {
    private final String phone;
    private final String code;
    private final String clientId;

    public SmsCodeAuthenticationToken(String phone, String code, String clientId) {
        super(Collections.emptyList());
        this.phone = phone;
        this.code = code;
        this.clientId = clientId;
        setAuthenticated(false);
    }

    public String getPhone() {
        return this.phone;
    }

    public String getCode() {
        return this.code;
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public Object getCredentials() {
        return this.code;
    }

    @Override
    public Object getPrincipal() {
        return this.phone;
    }
}
