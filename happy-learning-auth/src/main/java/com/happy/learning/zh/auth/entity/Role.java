package com.happy.learning.zh.auth.entity;

import lombok.Data;

import java.util.List;

@Data
public class Role {
    private Long id;
    private String name;
    private List<Permission> permissions;
}
