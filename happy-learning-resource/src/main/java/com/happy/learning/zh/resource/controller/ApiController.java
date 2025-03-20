package com.happy.learning.zh.resource.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// ApiController.java
@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/public/hello")
    public String publicHello() {
        return "Public Hello";
    }

    @GetMapping("/private/hello")
    public String privateHello() {
        return "Private Hello";
    }

    // 要求有 user:read 权限
    @PreAuthorize("hasPermission('user','user:read')")
    @GetMapping("/users")
    public String getUsers() {
        // 业务逻辑
        return "you are great!";
    }
}
