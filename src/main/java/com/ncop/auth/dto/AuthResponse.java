package com.ncop.auth.dto;

import java.util.Set;

public record AuthResponse(String token, String email, String fullName, Set<String> roles) {}