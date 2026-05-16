package com.project.userservice.controller;

import com.project.userservice.dto.*;
import com.project.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;


import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    @PostMapping("/internal")
    @Operation(summary = "Create user profile (internal — called by auth-service)")
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(request));
    }


    @GetMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.principal")
    @Operation(summary = "Get user by ID (own profile or admin)")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user's profile")
    public ResponseEntity<UserDto> getMe(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(userService.getUserById(UUID.fromString(userId)));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update current user's profile (PATCH — partial update)")
    public ResponseEntity<UserDto> updateMe(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(
                userService.updateUser(UUID.fromString(userId), request)
        );
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate current user account")
    public ResponseEntity<Void> deactivateMe(
            @RequestHeader("X-User-Id") String userId) {
        userService.deactivateUser(UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{id}/admin")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get any user by ID (admin only)")
    public ResponseEntity<UserDto> getUserByIdAdmin(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }


    @GetMapping("/me/saved-routes")
    @Operation(summary = "Get current user's saved routes")
    public ResponseEntity<List<SavedRouteDto>> getSavedRoutes(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(
                userService.getSavedRoutes(UUID.fromString(userId))
        );
    }

    @PostMapping("/me/saved-routes")
    @Operation(summary = "Save a frequent route")
    public ResponseEntity<SavedRouteDto> saveRoute(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody SavedRouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.saveRoute(UUID.fromString(userId), request));
    }

    @DeleteMapping("/me/saved-routes/{routeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a saved route")
    public ResponseEntity<Void> deleteSavedRoute(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID routeId) {
        userService.deleteSavedRoute(UUID.fromString(userId), routeId);
        return ResponseEntity.noContent().build();
    }
}
