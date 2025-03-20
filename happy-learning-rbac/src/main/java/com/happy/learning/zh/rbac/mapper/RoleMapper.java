package com.happy.learning.zh.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.happy.learning.zh.rbac.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    Set<String> findPermissionsByRoleName(@Param("roleName") String roleName);

    int updatePermissionsByRole(@Param("role") Role role);

    Set<String> findPermissionsByRoles(@Param("roles") List<String> roles);

    int deletePermissionsByRole(@Param("role")Role role);

    Set<String> findUsersByRoleId(@Param("roleId")Long roleId);
}
