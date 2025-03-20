package com.happy.learning.zh.resource.controller;

import com.happy.learning.zh.resource.entity.PermissionChangeEvent;
import com.happy.learning.zh.resource.service.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/rbac/permissions")
@RequiredArgsConstructor
public class PermissionChangeController {

    private final PermissionCacheService cacheService;

    @PostMapping("/cache/evict")
    public ResponseEntity userPermissionsEvictCache(@RequestBody PermissionChangeEvent event) {
        event.getUserIds().forEach(cacheService::evictCache);
        return ResponseEntity.ok().build();
    }
}
