package com.happy.learning.zh.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.happy.learning.zh.auth.entity.OAuth2AuthorizedTokens;
import org.apache.ibatis.annotations.Mapper;

// OAuth2ClientMapper.java
@Mapper
public interface OAuth2AuthorizedTokensMapper extends BaseMapper<OAuth2AuthorizedTokens> {
}

