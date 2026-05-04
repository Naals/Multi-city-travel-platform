package com.project.bookingservice.feign.dto;

import lombok.Data;

@Data
public class SeatLockResponse {
    private String  seatNumber;     // e.g. "14A"
    private boolean locked;
    private String  message;
}
