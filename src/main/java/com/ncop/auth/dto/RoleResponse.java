package com.ncop.auth.dto;

import java.time.Instant;
import java.util.List;

public record RoleResponse(
        String roleId,
        String name,
        String description,
        boolean active,
        List<String> moduleRights,
        long userCount,
        Instant createdOn,
        Instant lastUpdatedOn
) {
    public RoleResponse(String roleId, String name) {
        this(roleId, name, null, true, List.of(), 0L, null, null);
    }
}
