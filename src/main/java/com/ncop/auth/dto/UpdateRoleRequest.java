package com.ncop.auth.dto;

import java.util.List;

public record UpdateRoleRequest(
        String name,
        List<String> moduleRights
) {}
