package com.happy.learning.zh.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.happy.learning.zh.rbac.entity.Permission;
import com.happy.learning.zh.rbac.entity.Role;

import java.util.List;
import java.util.Set;

public interface IRbacService extends IService<Role> {
    /**
     * 获取角色权限列表
     * @param roleName
     * @return
     */
    Set<String> getPermissions(String roleName);

    /**
     * 更新角色权限列表
     * @param roleName
     * @param permissions
     */
    void updateRolePermissionsByName(String roleName, List<Permission> permissions);

    Set<String> getPermissions(List<String> roles);
}
