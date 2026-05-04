package com.project.bookingservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data @Builder
public class BookingResponse {
    private UUID              id;
    private UUID              tripId;
    private UUID              userId;
    private UUID              flightId;
    private String            flightNumber;
    private String            originIata;
    private String            destIata;
    private Instant           departureAt;
    private Instant           arrivalAt;
    private String            cabin;
    private BigDecimal        pricePaid;
    private String            currency;
    private String            seatNumber;
    private String            status;
    private List<Map<String,Object>> passengers;
    private Instant           bookedAt;
    private Instant           confirmedAt;
}
