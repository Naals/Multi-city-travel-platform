package com.project.bookingservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_events")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private BookingStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private BookingStatus toStatus;

    @Column(name = "triggered_by", nullable = false, length = 50)
    private String triggeredBy;   

    @Column
    private String note;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}