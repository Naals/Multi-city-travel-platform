package com.project.flightservice.kafka.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FlightUpdatedEvent {
    private String     eventType = "FLIGHT_UPDATED";
    private String     flightId;
    private String     flightNumber;
    private BigDecimal newPrice;
    private String     newStatus;
    private Instant    occurredAt = Instant.now();
}
