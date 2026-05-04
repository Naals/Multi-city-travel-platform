package com.project.bookingservice.repository;

import com.project.bookingservice.model.Booking;
import com.project.bookingservice.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserIdOrderByBookedAtDesc(UUID userId);

    List<Booking> findByTripIdOrderByDepartureAt(UUID tripId);

    List<Booking> findByFlightIdAndStatusIn(UUID flightId, List<BookingStatus> statuses);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.userId = :userId
          AND b.status IN ('PENDING','CONFIRMED')
        ORDER BY b.bookedAt DESC
    """)
    List<Booking> findActiveBookingsByUser(@Param("userId") UUID userId);

    @Modifying
    @Query("""
        UPDATE Booking b SET b.status = :status, b.updatedAt = CURRENT_TIMESTAMP
        WHERE b.id = :id
    """)
    int updateStatus(@Param("id") UUID id, @Param("status") BookingStatus status);

    @Modifying
    @Query("""
        UPDATE Booking b SET
            b.status = :status,
            b.confirmedAt = CURRENT_TIMESTAMP,
            b.updatedAt = CURRENT_TIMESTAMP
        WHERE b.id = :id AND b.status = 'PENDING'
    """)
    int confirmBooking(@Param("id") UUID id, @Param("status") BookingStatus status);

    // Bulk cancel all PENDING/CONFIRMED bookings for a cancelled flight
    @Modifying
    @Query("""
        UPDATE Booking b SET
            b.status = 'CANCELLED',
            b.cancellationReason = 'FLIGHT_CANCELLED',
            b.cancelledAt = CURRENT_TIMESTAMP
        WHERE b.flightId = :flightId
          AND b.status IN ('PENDING', 'CONFIRMED')
    """)
    int cancelAllForFlight(@Param("flightId") UUID flightId);
}
