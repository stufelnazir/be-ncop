package com.ncop.auth.controller;

import com.ncop.auth.dto.AuthResponse;
import com.ncop.auth.dto.LoginRequest;
import com.ncop.auth.model.Role;
import com.ncop.auth.model.User;
import com.ncop.auth.repository.RoleRepository;
import com.ncop.auth.repository.UserRepository;
import com.ncop.auth.service.UserService;
import com.ncop.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public AuthController(UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          UserService userService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        var userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPassword())) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", Instant.now());
            body.put("status", 401);
            body.put("error", "Unauthorized");
            body.put("message", "Invalid email or password");
            return ResponseEntity.status(401).body(body);
        }

        User user = userOpt.get();

        // Update last login date
        userService.updateLastLoginDate(user.getId());

        // Resolve role names from role IDs
        List<String> roleNames = roleRepository.findAllById(user.getRoleIds()).stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        String token = jwtUtil.generateToken(user.getEmail(), roleNames);

        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                Set.copyOf(roleNames),
                user.getUserType() != null ? user.getUserType().name() : null
        ));
    }
}