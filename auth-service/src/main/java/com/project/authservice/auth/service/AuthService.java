package com.project.authservice.auth.service;


import com.project.authservice.auth.dto.*;
import com.project.authservice.auth.exception.*;
import com.project.authservice.auth.model.*;
import com.project.authservice.auth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserCredentialRepository credentialRepo;
    private final AuthAuditLogRepository   auditRepo;
    private final TokenService             tokenService;
    private final PasswordEncoder          passwordEncoder;

    private static final int MAX_FAILED_ATTEMPTS = 5;


    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        if (credentialRepo.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException("Email already registered: " + email);
        }

        UserCredential credential = UserCredential.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.ROLE_USER)
                .active(true)
                .locked(false)
                .failedLoginAttempts(0)
                .build();

        credential = credentialRepo.save(credential);
        log.info("New user registered: {}", email);

        auditLog(credential.getId(), email, "REGISTER", null, null);

        return issueTokens(credential);
    }


    @Transactional
    public TokenResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        UserCredential credential = credentialRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    // Don't reveal whether email exists (timing attack prevention)
                    log.warn("Login attempt for non-existent email: {}", email);
                    return new BadCredentialsException("Invalid email or password");
                });

        // Account state checks
        if (!credential.isActive()) {
            throw new AccountDisabledException("Account is deactivated");
        }
        if (credential.isLocked()) {
            auditLog(credential.getId(), email, "LOGIN_FAIL_LOCKED",
                    request.getIpAddress(), request.getUserAgent());
            throw new AccountLockedException("Account is locked. Please contact support.");
        }

        // Password verification
        if (!passwordEncoder.matches(request.getPassword(), credential.getPasswordHash())) {
            handleFailedLogin(credential);
            auditLog(credential.getId(), email, "LOGIN_FAIL",
                    request.getIpAddress(), request.getUserAgent());
            throw new BadCredentialsException("Invalid email or password");
        }

        // Successful login — reset failure counter
        credentialRepo.resetFailedAttemptsAndUpdateLogin(credential.getId(), Instant.now());
        auditLog(credential.getId(), email, "LOGIN_SUCCESS",
                request.getIpAddress(), request.getUserAgent());

        return issueTokens(credential);
    }


    public TokenResponse refresh(RefreshRequest request) {
        // validateRefreshToken throws InvalidTokenException if expired/revoked
        String[] parts = tokenService.validateRefreshToken(request.getRefreshToken());
        String email = parts[0];
        String role  = parts[1];

        UserCredential credential = credentialRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidTokenException("User not found for refresh token"));

        if (!credential.isActive() || credential.isLocked()) {
            tokenService.revokeRefreshToken(request.getRefreshToken());
            throw new AccountDisabledException("Account is not active");
        }

        auditLog(credential.getId(), email, "TOKEN_REFRESH", null, null);

        // Issue a new access token only — refresh token stays valid
        String newAccessToken = tokenService.generateAccessToken(credential);
        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .expiresInMs(900_000L)
                .userId(credential.getId().toString())
                .email(email)
                .role(role)
                .build();
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        tokenService.blacklistToken(accessToken);
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenService.revokeRefreshToken(refreshToken);
        }
        try {
            var claims = tokenService.validateAccessToken(accessToken);
            auditLog(
                    java.util.UUID.fromString(claims.getSubject()),
                    claims.get("email", String.class),
                    "LOGOUT", null, null
            );
        } catch (Exception ignored) { /* Token already blacklisted above */ }
    }


    private TokenResponse issueTokens(UserCredential credential) {
        String accessToken  = tokenService.generateAccessToken(credential);
        String refreshToken = tokenService.generateRefreshToken(credential);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInMs(900_000L)
                .userId(credential.getId().toString())
                .email(credential.getEmail())
                .role(credential.getRole().name())
                .build();
    }


    private void handleFailedLogin(UserCredential credential) {
        credentialRepo.incrementFailedAttempts(credential.getId());
        int attempts = credential.getFailedLoginAttempts() + 1;
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            credentialRepo.lockAccount(credential.getId());
            auditLog(credential.getId(), credential.getEmail(),
                    "ACCOUNT_LOCKED", null, null);
            log.warn("Account locked after {} failed attempts: {}",
                    MAX_FAILED_ATTEMPTS, credential.getEmail());
        }
    }

    private void auditLog(java.util.UUID userId, String email,
                          String eventType, String ip, String userAgent) {
        auditRepo.save(AuthAuditLog.builder()
                .userId(userId)
                .email(email)
                .eventType(eventType)
                .ipAddress(ip)
                .userAgent(userAgent)
                .occurredAt(Instant.now())
                .build());
    }
}
