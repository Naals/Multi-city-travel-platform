package com.project.authservice.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;           // "Bearer"
    private long   expiresInMs;
    private String userId;
    private String email;
    private String role;
}