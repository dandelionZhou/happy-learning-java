package com.happy.learning.zh.auth.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PKCEGenerator {

    // 生成 code_verifier（随机字符串）
    public static String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32]; // 建议长度 32 字节（43-128字符）
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }

    // 生成 code_challenge（SHA-256 + Base64URL）
    public static String generateCodeChallenge(String codeVerifier) {
        try {
            byte[] bytes = codeVerifier.getBytes();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(bytes);
            byte[] digest = md.digest();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static void main(String[] args) {
        // 1. 生成 code_verifier
        String codeVerifier = generateCodeVerifier();
        System.out.println("Code Verifier: " + codeVerifier);

        // 2. 生成 code_challenge
        String codeChallenge = generateCodeChallenge(codeVerifier);
        System.out.println("Code Challenge: " + codeChallenge);
    }
}

