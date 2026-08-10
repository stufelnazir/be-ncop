package com.ncop.auth.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateRoleRequest(
        @NotBlank(message = "Role name is required") String name,
        List<String> moduleRights
) {}
