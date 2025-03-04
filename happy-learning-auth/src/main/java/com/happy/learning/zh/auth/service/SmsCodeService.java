package com.happy.learning.zh.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SmsCodeService {
    private final StringRedisTemplate redisTemplate;

    // 生成并存储验证码
    public void sendCode(String phone) {
        String code = String.valueOf(new Random().nextInt(899999) + 100000);
        redisTemplate.opsForValue().set("SMS_CODE:" + phone, code, 5, TimeUnit.MINUTES);
        // 调用短信服务商 API 发送验证码（此处模拟）
        System.out.println("发送验证码：" + code + " 到手机：" + phone);
    }

    // 验证码校验
    public boolean validateCode(String phone, String code) {
        String storedCode = redisTemplate.opsForValue().get("SMS_CODE:" + phone);
        if (code != null && code.equals(storedCode)) {
            redisTemplate.delete("SMS_CODE:" + phone);
            return true;
        }
        return false;
    }
}