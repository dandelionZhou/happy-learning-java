package com.happy.learning.zh.auth.mapper;

import com.happy.learning.zh.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    List<User> findByUsernameWithRoles(@Param("username") String username);
}
