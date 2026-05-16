package com.project.flightservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlightAvailabilityResponse {
    private UUID flightId;
    private int availableSeats;
    private int totalSeats;
}
