package com.project.reviewservice.repository;

import com.project.reviewservice.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByFlightIdAndPublishedTrueOrderByCreatedAtDesc(UUID flightId);

    List<Review> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Review> findByBookingId(UUID bookingId);

    boolean existsByBookingId(UUID bookingId);

    @Query("""
        SELECT AVG(r.overallRating) FROM Review r
        WHERE r.flightId = :flightId AND r.published = true
    """)
    Double findAvgRatingByFlightId(@Param("flightId") UUID flightId);

    @Query("""
        SELECT COUNT(r) FROM Review r
        WHERE r.flightId = :flightId AND r.published = true
    """)
    Long countByFlightId(@Param("flightId") UUID flightId);
}
