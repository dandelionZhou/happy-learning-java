package com.happy.learning.zh.auth.entity;

import lombok.Data;

@Data
public class Permission {
    private Long id;
    private Long pid;
    private String name;
    private String value;
}
