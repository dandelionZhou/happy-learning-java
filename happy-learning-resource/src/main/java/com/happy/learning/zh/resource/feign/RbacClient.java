package com.happy.learning.zh.resource.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Set;

@FeignClient(name = "rbac-service", url = "${rbac.service.url}")
public interface RbacClient {
    @PostMapping("/rbac/permissions")
    Set<String> getPermissions(@RequestBody List<String> role);
}
