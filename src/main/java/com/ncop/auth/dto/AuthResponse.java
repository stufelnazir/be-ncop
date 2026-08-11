package com.ncop.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    @JsonProperty("token")
    private String token;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expiresIn")
    private long expiresIn; // in seconds

    @JsonProperty("email")
    private String email;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("roles")
    private Set<String> roles;

    @JsonProperty("userType")
    private String userType;

    @JsonProperty("moduleRights")
    private List<ModuleRightResponse> moduleRights;

    @JsonProperty("lastLoginDate")
    private Instant lastLoginDate;

    @JsonProperty("lastLoginDateUtcDateTimeFormatted")
    private String lastLoginDateUtcDateTimeFormatted;

    @JsonProperty("lastLoginDateCurrentTimezoneDateFormatted")
    private String lastLoginDateCurrentTimezoneDateFormatted;
}
