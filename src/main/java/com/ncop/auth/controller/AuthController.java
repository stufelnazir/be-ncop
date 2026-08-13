package com.ncop.auth.controller;

import com.ncop.auth.dto.AuthResponse;
import com.ncop.auth.dto.ErrorResponse;
import com.ncop.auth.dto.LoginRequest;
import com.ncop.auth.dto.ModuleRightResponse;
import com.ncop.auth.model.ModuleRight;
import com.ncop.auth.model.Role;
import com.ncop.auth.model.User;
import com.ncop.auth.repository.ModuleRightRepository;
import com.ncop.auth.repository.RoleRepository;
import com.ncop.auth.repository.UserRepository;
import com.ncop.auth.service.UserService;
import com.ncop.auth.util.DateTimeFormatterUtil;
import com.ncop.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ModuleRightRepository moduleRightRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public AuthController(UserRepository userRepository,
                          RoleRepository roleRepository,
                          ModuleRightRepository moduleRightRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          UserService userService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.moduleRightRepository = moduleRightRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        var userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPassword())) {
            ErrorResponse error = new ErrorResponse(401, "Unauthorized", "Invalid email or password");
            return ResponseEntity.status(401).body(error);
        }

        User user = userOpt.get();

        // Update last login date
        userService.updateLastLoginDate(user.getId());
        user.setLastLoginDate(userService.getUserById(user.getId()).getLastLoginDate());

        // Resolve roles from role IDs
        List<Role> roles = roleRepository.findAllById(user.getRoleIds());
        List<String> roleNames = roles.stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        List<ModuleRightResponse> moduleRights = buildModuleRightResponses(user);

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), roleNames);
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        long expiresIn = jwtUtil.getAccessTokenExpiryMs() / 1000; // Convert to seconds

        // Format last login date
        String lastLoginDateUtcFormatted = DateTimeFormatterUtil.formatToUtcDateTime(user.getLastLoginDate());
        String lastLoginDateTimezoneFormatted = DateTimeFormatterUtil.formatToCurrentTimezoneDateTime(user.getLastLoginDate());

        AuthResponse response = new AuthResponse();
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(expiresIn);
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRoles(new HashSet<>(roleNames));
        response.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        response.setModuleRights(moduleRights);
        response.setLastLoginDate(user.getLastLoginDate());
        response.setLastLoginDateUtcDateTimeFormatted(lastLoginDateUtcFormatted);
        response.setLastLoginDateCurrentTimezoneDateFormatted(lastLoginDateTimezoneFormatted);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ErrorResponse error = new ErrorResponse(401, "Unauthorized", "Refresh token required in Authorization header");
            return ResponseEntity.status(401).body(error);
        }

        String refreshToken = authHeader.substring(7);

        try {
            String email = jwtUtil.extractUsername(refreshToken);
            String tokenType = jwtUtil.getTokenType(refreshToken);

            if (!"refresh".equals(tokenType)) {
                ErrorResponse error = new ErrorResponse(401, "Unauthorized", "Invalid token type. Refresh token required");
                return ResponseEntity.status(401).body(error);
            }

            var userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                ErrorResponse error = new ErrorResponse(404, "Not Found", "User not found");
                return ResponseEntity.status(404).body(error);
            }

            User user = userOpt.get();

            // Resolve roles from role IDs
            List<Role> roles = roleRepository.findAllById(user.getRoleIds());
            List<String> roleNames = roles.stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());

            List<ModuleRightResponse> moduleRights = buildModuleRightResponses(user);

            // Generate new access token
            String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), roleNames);
            long expiresIn = jwtUtil.getAccessTokenExpiryMs() / 1000; // Convert to seconds

            // Format last login date
            String lastLoginDateUtcFormatted = DateTimeFormatterUtil.formatToUtcDateTime(user.getLastLoginDate());
            String lastLoginDateTimezoneFormatted = DateTimeFormatterUtil.formatToCurrentTimezoneDateTime(user.getLastLoginDate());

            AuthResponse response = new AuthResponse();
            response.setToken(newAccessToken);
            response.setRefreshToken(refreshToken); // Return same refresh token
            response.setExpiresIn(expiresIn);
            response.setEmail(user.getEmail());
            response.setFirstName(user.getFirstName());
            response.setLastName(user.getLastName());
            response.setRoles(new HashSet<>(roleNames));
            response.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
            response.setModuleRights(moduleRights);
            response.setLastLoginDate(user.getLastLoginDate());
            response.setLastLoginDateUtcDateTimeFormatted(lastLoginDateUtcFormatted);
            response.setLastLoginDateCurrentTimezoneDateFormatted(lastLoginDateTimezoneFormatted);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse(401, "Unauthorized", "Invalid or expired refresh token");
            return ResponseEntity.status(401).body(error);
        }
    }

    private List<ModuleRightResponse> buildModuleRightResponses(User user) {
        List<ModuleRight> allModuleRights = moduleRightRepository.findAll();
        List<String> assignedModuleRights = user.getModuleRights() != null ? user.getModuleRights() : new ArrayList<>();

        return allModuleRights.stream()
                .map(moduleRight -> new ModuleRightResponse(
                        moduleRight.getName(),
                        moduleRight.getLabel() != null ? moduleRight.getLabel() : moduleRight.getName(),
                        assignedModuleRights.contains(moduleRight.getName())
                ))
                .toList();
    }
}