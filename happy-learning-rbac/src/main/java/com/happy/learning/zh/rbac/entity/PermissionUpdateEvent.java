package com.happy.learning.zh.rbac.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class PermissionUpdateEvent {
    private Set<String> userIds;
}
