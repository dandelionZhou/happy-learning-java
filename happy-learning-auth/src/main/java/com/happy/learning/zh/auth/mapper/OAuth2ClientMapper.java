package com.happy.learning.zh.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.happy.learning.zh.auth.entity.OAuth2Client;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

// OAuth2ClientMapper.java
@Mapper
public interface OAuth2ClientMapper extends BaseMapper<OAuth2Client> {
    @Select("SELECT * FROM t_oauth2_client WHERE client_id = #{clientId}")
    OAuth2Client findByClientId(String clientId);
}

