package com.project.reviewservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "review_eligibility",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_eligibility",
                columnNames = {"user_id", "flight_id", "booking_id"}
        )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewEligibility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "flight_id", nullable = false)
    private UUID flightId;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "eligible_at", nullable = false)
    private Instant eligibleAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean reviewed = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
