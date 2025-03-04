package com.happy.learning.zh.auth.entity;

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
@TableName("t_oauth2_client")
public class OAuth2Client {
    @TableId
    private String id;
    private String clientId;
    private String clientSecret;
    private String clientAuthenticationMethods;
    private String authorizationGrantTypes;
    private String redirectUris;
    private String scopes;
    private String clientSettings;
    private String tokenSettings;
    private Date createTime;
    private Date updateTime;
    private String status;
}