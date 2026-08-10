package com.ncop.auth.dto;

import com.ncop.auth.enums.UserStatus;
import com.ncop.auth.enums.UserType;
import jakarta.validation.constraints.Email;

import java.util.List;

public record UpdateUserRequest(
        @Email(message = "Must be a valid email address")
        String email,

        String firstName,
        String lastName,
        List<String> roleIds,
        UserStatus userStatus,
        UserType userType
) {}
