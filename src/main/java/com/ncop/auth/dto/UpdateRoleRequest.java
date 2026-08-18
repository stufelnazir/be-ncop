package com.ncop.auth.dto;

import java.util.List;

public record UpdateRoleRequest(
        String name,
        String description,
        Boolean active,
        List<String> moduleRights
) {
    public UpdateRoleRequest(String name) {
        this(name, null, null, null);
    }
}
