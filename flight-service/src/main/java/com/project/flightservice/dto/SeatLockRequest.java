package com.project.flightservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatLockRequest {

    private UUID bookingId;
    private String passengerName;
}
