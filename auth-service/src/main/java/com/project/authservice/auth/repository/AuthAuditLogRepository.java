package com.project.authservice.auth.repository;

import com.project.authservice.auth.model.AuthAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Long> {

    List<AuthAuditLog> findTop10ByUserIdOrderByOccurredAtDesc(UUID userId);

    long countByUserIdAndEventTypeAndOccurredAtAfter(
            UUID userId, String eventType, java.time.Instant since
    );
}