package com.happy.learning.zh.rbac.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_user_permission")
public class Permission {
    private Long id;
    private Long pid;
    private String name;
    private String value;
}
