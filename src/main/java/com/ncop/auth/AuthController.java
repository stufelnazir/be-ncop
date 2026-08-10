package com.ncop.auth;

import com.ncop.auth.dto.AuthResponse;
import com.ncop.auth.dto.LoginRequest;
import com.ncop.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        var userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }

        User user = userOpt.get();
        List<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList());

        String token = jwtUtil.generateToken(user.getEmail(), roleNames);

        return ResponseEntity.ok(new AuthResponse(
                token, user.getEmail(), user.getFullName(), Set.copyOf(roleNames)
        ));
    }
}