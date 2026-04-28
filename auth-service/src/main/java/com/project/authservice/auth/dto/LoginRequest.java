package com.project.authservice.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    // Optional: client info for audit log
    private String ipAddress;
    private String userAgent;
}
