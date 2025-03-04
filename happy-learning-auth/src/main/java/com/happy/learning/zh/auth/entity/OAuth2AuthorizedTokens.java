package com.happy.learning.zh.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

// OAuth2Client.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_oauth2_authorized_tokens")
public class OAuth2AuthorizedTokens {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    //客户端id
    private String clientId;
    private String tokenValue;
    private String tokenType;
    private Long issuedAt;
    private Long expiresAt;
    private Integer isRevoked;
    private Date createTime;
    private Date updateTime;
}