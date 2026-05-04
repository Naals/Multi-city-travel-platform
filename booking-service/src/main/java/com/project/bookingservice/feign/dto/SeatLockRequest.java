package com.project.bookingservice.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@Data
@AllArgsConstructor
public class SeatLockRequest {
    private UUID   bookingId;
    private String cabin;
    private int    quantity;        // usually 1 per passenger
}