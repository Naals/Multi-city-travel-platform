package com.project.userservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saved_routes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavedRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "origin_city_code", nullable = false, length = 10)
    private String originCityCode;

    @Column(name = "dest_city_code", nullable = false, length = 10)
    private String destCityCode;

    @Column(length = 100)
    private String label;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
