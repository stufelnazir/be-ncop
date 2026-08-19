package com.ncop.auth.service;

import com.ncop.auth.model.Role;
import com.ncop.auth.repository.RoleRepository;
import com.ncop.auth.model.User;
import com.ncop.auth.repository.UserRepository;
import com.ncop.auth.dto.CreateUserRequest;
import com.ncop.auth.dto.UpdateUserRequest;
import com.ncop.auth.dto.UserResponse;
import com.ncop.auth.enums.UserStatus;
import com.ncop.auth.enums.UserType;
import com.ncop.auth.exception.DuplicateResourceException;
import com.ncop.auth.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User with email '" + request.email() + "' already exists");
        }

        // Validate that all provided role IDs exist
        validateRoleIds(request.roleIds());

        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.email());  // Business Rule: username = email
        user.setPassword(passwordEncoder.encode(request.password()));  // BCrypt hash
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRoleIds(request.roleIds() != null ? request.roleIds() : new ArrayList<>());
        user.setModuleRights(request.moduleRights() != null ? request.moduleRights() : new ArrayList<>());
        user.setUserStatus(request.userStatus() != null ? request.userStatus() : UserStatus.PENDING);
        user.setUserType(request.userType() != null ? request.userType() : UserType.EMPLOYEE);

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Override
    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new DuplicateResourceException("User with email '" + request.email() + "' already exists");
            }
            user.setEmail(request.email());
            user.setUsername(request.email());  // Business Rule: sync username = email
        }

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.roleIds() != null) {
            validateRoleIds(request.roleIds());
            user.setRoleIds(request.roleIds());
        }
        if (request.moduleRights() != null) {
            user.setModuleRights(request.moduleRights());
        }
        if (request.userStatus() != null) {
            user.setUserStatus(request.userStatus());
        }
        if (request.userType() != null) {
            user.setUserType(request.userType());
        }

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Override
    public UserResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return toResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public com.ncop.common.dto.PageResponse<UserResponse> getUsers(org.springframework.data.domain.Pageable pageable, String search, String status, String role) {
        List<UserResponse> all = userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();

        // Apply filters
        List<UserResponse> filtered = all.stream().filter(u -> {
            boolean matchesSearch = search == null || search.isBlank() ||
                    (u.getEmail() != null && u.getEmail().toLowerCase().contains(search.toLowerCase())) ||
                    (u.getFullName() != null && u.getFullName().toLowerCase().contains(search.toLowerCase())) ||
                    (u.getUsername() != null && u.getUsername().toLowerCase().contains(search.toLowerCase())) ||
                    (u.getRoleNames() != null && u.getRoleNames().stream().anyMatch(r -> r.toLowerCase().contains(search.toLowerCase())));

            boolean matchesStatus = true;
            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                if ("ROLE_INACTIVE".equalsIgnoreCase(status)) {
                    matchesStatus = !u.isHasActiveRole() && u.getRoleIds() != null && !u.getRoleIds().isEmpty();
                } else {
                    matchesStatus = u.getUserStatus() != null && u.getUserStatus().name().equalsIgnoreCase(status);
                }
            }

            boolean matchesRole = true;
            if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
                matchesRole = u.getRoleNames() != null && u.getRoleNames().stream().anyMatch(r -> r.equalsIgnoreCase(role));
            }

            return matchesSearch && matchesStatus && matchesRole;
        }).toList();

        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int fromIndex = Math.min(pageNumber * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<UserResponse> paged = filtered.subList(fromIndex, toIndex);

        return com.ncop.common.dto.PageResponse.of(paged, pageNumber, pageSize, filtered.size());
    }

    @Override
    public void deleteUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
    }

    @Override
    public void updateLastLoginDate(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setLastLoginDate(Instant.now());
        userRepository.save(user);
    }

    @Override
    public void resetPassword(String userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ── Private helpers ──────────────────────────────────────────────

    private void validateRoleIds(List<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return;
        for (String roleId : roleIds) {
            if (!roleRepository.existsById(roleId)) {
                throw new ResourceNotFoundException("Role not found with id: " + roleId);
            }
        }
    }

    private UserResponse toResponse(User user) {
        // Format dates in user object
        user.formatAllDates();

        // Resolve role names and active status from role IDs
        List<String> roleNames = new ArrayList<>();
        boolean hasActiveRole = true;
        if (user.getRoleIds() != null && !user.getRoleIds().isEmpty()) {
            List<Role> roles = roleRepository.findAllById(user.getRoleIds());
            roleNames = roles.stream().map(Role::getName).toList();
            hasActiveRole = roles.stream().anyMatch(Role::isActive);
        }

        String fullName = buildFullName(user.getFirstName(), user.getLastName());
        boolean effectiveActive = (user.getUserStatus() == UserStatus.ACTIVE) && hasActiveRole;

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setFullName(fullName);
        response.setRoleIds(user.getRoleIds());
        response.setRoleNames(roleNames);
        response.setModuleRights(user.getModuleRights() != null ? new ArrayList<>(user.getModuleRights()) : new ArrayList<>());
        response.setUserStatus(user.getUserStatus());
        response.setUserType(user.getUserType());
        response.setHasActiveRole(hasActiveRole);
        response.setEffectiveActive(effectiveActive);
        response.setCreatedOn(user.getCreatedOn());
        response.setLastUpdatedOn(user.getLastUpdatedOn());
        response.setLastLoginDate(user.getLastLoginDate());

        // Format all date fields
        response.formatAllDates();

        return response;
    }

    private String buildFullName(String firstName, String lastName) {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) sb.append(firstName);
        if (lastName != null && !lastName.isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(lastName);
        }
        return sb.toString();
    }
}
