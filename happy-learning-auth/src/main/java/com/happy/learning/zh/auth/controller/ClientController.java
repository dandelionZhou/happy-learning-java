package com.happy.learning.zh.auth.controller;

import com.happy.learning.zh.auth.service.ClientRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth2/client")
@RequiredArgsConstructor
public class ClientController {
    private final ClientRegistrationService clientRegistrationService;

    @GetMapping("/register")
    public String registerClient() {
        clientRegistrationService.registerClient("web-test", "web-test-secret");
        return "ok";
    }
}
