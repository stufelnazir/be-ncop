package com.ncop.auth.dto;

import com.ncop.auth.enums.UserStatus;
import com.ncop.auth.enums.UserType;

import java.time.Instant;
import java.util.List;

public record UserResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        String fullName,
        List<String> roleIds,
        List<String> roleNames,
        UserStatus userStatus,
        UserType userType,
        Instant createdOn,
        Instant lastUpdatedOn,
        Instant lastLoginDate
) {}
