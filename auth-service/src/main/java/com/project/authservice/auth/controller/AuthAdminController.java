package com.project.authservice.auth.controller;

import com.project.authservice.auth.repository.UserCredentialRepository;
import com.project.authservice.auth.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Admin", description = "Admin-only token management")
public class AuthAdminController {

    private final TokenService             tokenService;
    private final UserCredentialRepository credentialRepo;

    @PostMapping("/revoke-all/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Revoke ALL sessions for a user (admin)")
    public ResponseEntity<Void> revokeAll(@PathVariable UUID userId) {
        tokenService.revokeAllRefreshTokens(userId.toString());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/unlock/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unlock a locked account")
    public ResponseEntity<Void> unlockAccount(@PathVariable UUID userId) {
        credentialRepo.findById(userId).ifPresent(cred -> {
            cred.setLocked(false);
            cred.setFailedLoginAttempts(0);
            credentialRepo.save(cred);
        });
        return ResponseEntity.noContent().build();
    }
}
