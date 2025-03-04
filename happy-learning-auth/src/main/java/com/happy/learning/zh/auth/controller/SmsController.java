package com.happy.learning.zh.auth.controller;

import com.happy.learning.zh.auth.service.SmsCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SmsController {
    @Autowired
    private SmsCodeService smsCodeService;

    @GetMapping("/sms/code")
    public ResponseEntity<?> sendCode(@RequestParam String phone) {
        smsCodeService.sendCode(phone);
        return ResponseEntity.ok().build();
    }
}
