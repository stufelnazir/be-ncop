package com.ncop.auth.dto;

import java.util.List;

public record RoleResponse(
        String roleId,
        String name,
        List<String> moduleRights
) {}
