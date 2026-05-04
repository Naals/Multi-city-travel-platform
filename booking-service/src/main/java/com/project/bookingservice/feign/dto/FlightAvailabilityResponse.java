package com.project.bookingservice.feign.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class FlightAvailabilityResponse {
    private UUID       flightId;
    private String     flightNumber;
    private String     airlineCode;
    private String     airlineName;
    private String     originIata;
    private String     destIata;
    private Instant    departureAt;
    private Instant    arrivalAt;
    private String     cabin;
    private int        seatsAvailable;
    private BigDecimal currentPrice;
    private String     currency;
    private String     status;
    private boolean    bookable;
}