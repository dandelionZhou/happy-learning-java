package com.happy.learning.zh.rbac.feign;

import com.happy.learning.zh.rbac.entity.PermissionUpdateEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Set;

@FeignClient(name = "resource-service", url = "${resource.service.url}")
public interface ResourceClient {
    @PostMapping("/rbac/permissions/cache/evict")
    String userPermissionsEvictCache(@RequestBody PermissionUpdateEvent event);
}
