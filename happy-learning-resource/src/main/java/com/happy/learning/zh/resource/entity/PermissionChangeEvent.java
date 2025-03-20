package com.happy.learning.zh.resource.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class PermissionChangeEvent {
    private Set<String> userIds;
}
