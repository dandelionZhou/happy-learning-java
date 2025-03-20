package com.happy.learning.zh.rbac.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;

@Data
@TableName("t_user")
public class User {
    private String id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private Boolean enabled;
    private List<Role> roles;
}
