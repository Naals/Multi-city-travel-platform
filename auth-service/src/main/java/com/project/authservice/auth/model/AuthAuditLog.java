package com.project.authservice.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_audit_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    private String email;

    @Column(name = "event_type", nullable = false)
    private String eventType;   

    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();
}