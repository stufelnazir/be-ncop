package com.ncop.auth.service;

import com.ncop.auth.dto.CreateUserRequest;
import com.ncop.auth.dto.UpdateUserRequest;
import com.ncop.auth.dto.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(String userId, UpdateUserRequest request);

    UserResponse getUserById(String userId);

    List<UserResponse> getAllUsers();

    void deleteUser(String userId);

    void updateLastLoginDate(String userId);

    void resetPassword(String userId, String newPassword);
}
