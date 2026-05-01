package com.project.flightservice.kafka.event;

import lombok.*;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FlightDelayedEvent {
    private String  eventType   = "FLIGHT_DELAYED";
    private String  flightId;
    private String  flightNumber;
    private String  airlineCode;
    private String  originIata;
    private String  destIata;
    private Instant originalDeparture;
    private Instant newDeparture;
    private int     delayMinutes;
    private String  reason;
    private Instant occurredAt  = Instant.now();
}
