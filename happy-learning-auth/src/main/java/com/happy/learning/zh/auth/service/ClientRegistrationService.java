package com.happy.learning.zh.auth.service;

import com.happy.learning.zh.auth.entity.OAuth2Client;
import com.happy.learning.zh.auth.mapper.OAuth2ClientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

// 客户端注册逻辑
@Service
@RequiredArgsConstructor
public class ClientRegistrationService {

    private final OAuth2ClientMapper auth2ClientMapper;
    private final PasswordEncoder passwordEncoder;

    public void registerClient(String clientId, String rawSecret) {
        // 生成加密后的密码
        String encodedSecret = passwordEncoder.encode(rawSecret);

        // 实现插入逻辑（根据业务需求补充）
        OAuth2Client client = new OAuth2Client();
        client.setId(UUID.randomUUID().toString());
        client.setClientId(clientId);
        client.setClientSecret("{bcrypt}" + passwordEncoder.encode(rawSecret));
        client.setClientSettings("{\"requireProofKey\":true}");
        client.setClientAuthenticationMethods("[\"client_secret_basic\"]");
        client.setAuthorizationGrantTypes("[\"authorization_code\",\"refresh_token\",\"client_credentials\"]");
        client.setRedirectUris("[\"https://example.com/callback\"]");
        client.setScopes("[\"read\", \"write\"]'");
        client.setTokenSettings("{\"accessTokenTimeToLive\":3600}");
        client.setStatus("ENABLED");
        auth2ClientMapper.insert(client);

    }
}
