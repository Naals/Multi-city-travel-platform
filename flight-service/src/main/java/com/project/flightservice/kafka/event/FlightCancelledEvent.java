package com.project.flightservice.kafka.event;

import lombok.*;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FlightCancelledEvent {
    private String  eventType  = "FLIGHT_CANCELLED";
    private String  flightId;
    private String  flightNumber;
    private String  airlineCode;
    private String  originIata;
    private String  destIata;
    private Instant scheduledDeparture;
    private String  cancellationReason;
    private Instant occurredAt = Instant.now();
}
