package com.project.authservice.auth.repository;

import com.project.authservice.auth.model.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {

    Optional<UserCredential> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Modifying
    @Query("UPDATE UserCredential u SET u.failedLoginAttempts = 0, u.lastLoginAt = :now WHERE u.id = :id")
    void resetFailedAttemptsAndUpdateLogin(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE UserCredential u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :id")
    void incrementFailedAttempts(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE UserCredential u SET u.locked = true WHERE u.id = :id")
    void lockAccount(@Param("id") UUID id);
}