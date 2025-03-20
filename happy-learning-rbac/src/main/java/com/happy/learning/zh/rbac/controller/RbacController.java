package com.happy.learning.zh.rbac.controller;

import com.happy.learning.zh.rbac.entity.Permission;
import com.happy.learning.zh.rbac.service.IRbacService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

// RbacController.java
@RestController
@RequestMapping("/rbac")
@RequiredArgsConstructor
public class RbacController {

    private final IRbacService rbacService;

    @PostMapping("/permissions")
    public ResponseEntity<Set<String>> getPermissions(@RequestBody List<String> roles) {
        return ResponseEntity.ok(rbacService.getPermissions(roles));
    }

    @PostMapping("/roles/{roleName}/permissions")
    public void updateRolePermissionsByName(@PathVariable String roleName, @RequestBody List<Permission> permissions) {
        rbacService.updateRolePermissionsByName(roleName, permissions);
    }

}
