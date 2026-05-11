package com.project.reviewservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eligibility_id", nullable = false)
    private ReviewEligibility eligibility;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "flight_id", nullable = false)
    private UUID flightId;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    // ── Ratings 1-5 ───────────────────────────────────────────
    @Column(name = "overall_rating", nullable = false)
    private Short overallRating;

    @Column(name = "punctuality_rating")
    private Short punctualityRating;

    @Column(name = "comfort_rating")
    private Short comfortRating;

    @Column(name = "service_rating")
    private Short serviceRating;

    @Column(name = "value_rating")
    private Short valueRating;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private boolean published = true;

    @Column(name = "is_flagged", nullable = false)
    @Builder.Default
    private boolean flagged = false;

    @Column(name = "flag_reason", length = 200)
    private String flagReason;

    @Column(name = "helpful_votes", nullable = false)
    @Builder.Default
    private int helpfulVotes = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}