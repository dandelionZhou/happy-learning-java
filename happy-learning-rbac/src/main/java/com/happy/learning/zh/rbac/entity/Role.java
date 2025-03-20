package com.happy.learning.zh.rbac.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;

@Data
@TableName("t_user_role")
public class Role {
    private Long id;
    private String name;
    @TableField(exist = false)
    private List<Permission> permissions;
}
