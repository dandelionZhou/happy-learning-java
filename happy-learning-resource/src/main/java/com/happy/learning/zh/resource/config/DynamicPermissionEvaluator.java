package com.happy.learning.zh.resource.config;

import com.happy.learning.zh.resource.service.PermissionCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DynamicPermissionEvaluator implements PermissionEvaluator {

    private final PermissionCacheService permissionCacheService;

    // 通过构造函数注入（推荐方式）
    @Autowired
    public DynamicPermissionEvaluator(PermissionCacheService permissionCacheService) {
        this.permissionCacheService = permissionCacheService;
    }
    /**
     * 验证权限（支持 SpEL 表达式如 @PreAuthorize("hasPermission('user:read')")）
     */
    @Override
    public boolean hasPermission(
            Authentication authentication,
            Object targetDomainObject,
            Object permission
    ) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getSubject();
        List<String> roles = jwt.getClaimAsStringList("roles");

        // 获取用户权限（带缓存）
        Set<String> permissions = permissionCacheService.getPermissions(userId, roles.stream().map(r -> r.replaceAll("ROLE_", "")).collect(Collectors.toList()));

        // 检查权限是否存在
        return permissions.contains(permission.toString());
    }

    @Override
    public boolean hasPermission(
            Authentication authentication,
            Serializable targetId,
            String targetType,
            Object permission
    ) {
        // 示例实现：根据资源 ID 和类型验证权限
        String requiredPermission = targetType + ":" + permission;
        return hasPermission(authentication, null, requiredPermission);
    }
}