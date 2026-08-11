package com.ncop.auth.dto;

import com.ncop.auth.enums.UserStatus;
import com.ncop.auth.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateUserRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password,

        String firstName,
        String lastName,
        List<String> roleIds,
        List<String> moduleRights,
        UserStatus userStatus,
        UserType userType
) {}
