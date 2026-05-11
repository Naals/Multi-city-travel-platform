package com.project.reviewservice.repository;

import com.project.reviewservice.model.ReviewEligibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewEligibilityRepository
        extends JpaRepository<ReviewEligibility, UUID> {

    Optional<ReviewEligibility> findByUserIdAndBookingId(UUID userId, UUID bookingId);

    Optional<ReviewEligibility> findByBookingId(UUID bookingId);

    boolean existsByUserIdAndFlightIdAndBookingId(
            UUID userId, UUID flightId, UUID bookingId);

    @Modifying
    @Query("""
        UPDATE ReviewEligibility e
        SET e.reviewed = true
        WHERE e.id = :id
    """)
    void markReviewed(@Param("id") UUID id);
}
