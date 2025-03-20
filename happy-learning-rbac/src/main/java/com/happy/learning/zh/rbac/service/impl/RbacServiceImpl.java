package com.happy.learning.zh.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.happy.learning.zh.rbac.entity.Permission;
import com.happy.learning.zh.rbac.entity.PermissionUpdateEvent;
import com.happy.learning.zh.rbac.entity.Role;
import com.happy.learning.zh.rbac.exception.RoleNotFoundException;
import com.happy.learning.zh.rbac.mapper.RoleMapper;
import com.happy.learning.zh.rbac.notification.NotificationPublisher;
import com.happy.learning.zh.rbac.service.IRbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class RbacServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRbacService {
    private final NotificationPublisher notifier;

    @Override
    @Cacheable(value = "rbac", key = "#roleName")
    public Set<String> getPermissions(String roleName) {
        return Optional.ofNullable(baseMapper.findPermissionsByRoleName(roleName)).orElseThrow(() -> new RoleNotFoundException(roleName));
    }

    @Override
    @Transactional
    @CacheEvict(value = "rbac", key = "#roleName")
    public void updateRolePermissionsByName(String roleName, List<Permission> permissions) {
        Role role = baseMapper.selectOne(new QueryWrapper<Role>().lambda().eq(Role::getName, roleName));
        Optional.ofNullable(role).orElseThrow(() -> new RoleNotFoundException(roleName));
        role.setPermissions(permissions);
        baseMapper.deletePermissionsByRole(role);
        baseMapper.updatePermissionsByRole(role);
        Set<String> userIds = baseMapper.findUsersByRoleId(role.getId());
        notifier.publish(new PermissionUpdateEvent(userIds));
    }

    @Override
    @Cacheable(value = "rbac", key = "#roles.hashCode()")
    public Set<String> getPermissions(List<String> roles) {
        return Optional.ofNullable(baseMapper.findPermissionsByRoles(roles)).orElseThrow(() -> new RoleNotFoundException(roles.toString()));
    }
}
