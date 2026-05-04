package com.project.bookingservice.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;                    // FK to user-service (cross-DB)

    @Column(name = "flight_id", nullable = false)
    private UUID flightId;                  // FK to flight-service (cross-DB)


    @Column(name = "flight_number", nullable = false, length = 10)
    private String flightNumber;

    @Column(name = "origin_iata", nullable = false, length = 3)
    private String originIata;

    @Column(name = "dest_iata", nullable = false, length = 3)
    private String destIata;

    @Column(name = "departure_at", nullable = false)
    private Instant departureAt;

    @Column(name = "arrival_at", nullable = false)
    private Instant arrivalAt;

    @Column(nullable = false, length = 20)
    private String cabin;

    @Column(name = "price_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePaid;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "seat_number", length = 5)
    private String seatNumber;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> passengers = new java.util.ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason")
    private CancellationReason cancellationReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_note")
    private String cancelNote;

    @CreationTimestamp
    @Column(name = "booked_at", updatable = false)
    private Instant bookedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
