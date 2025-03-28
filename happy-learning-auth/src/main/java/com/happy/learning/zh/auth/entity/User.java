package com.happy.learning.zh.auth.entity;

import lombok.Data;

import java.util.List;

@Data
public class User {
    private String id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private boolean enabled = true;
    private List<Role> roles;
}
