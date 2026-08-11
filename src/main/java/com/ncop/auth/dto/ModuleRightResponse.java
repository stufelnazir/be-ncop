package com.ncop.auth.dto;

public record ModuleRightResponse(
        String name,
        String label,
        boolean visible
) {}
