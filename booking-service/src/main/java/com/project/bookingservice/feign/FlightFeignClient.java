package com.project.bookingservice.feign;

import com.project.bookingservice.feign.dto.FlightAvailabilityResponse;
import com.project.bookingservice.feign.dto.SeatLockRequest;
import com.project.bookingservice.feign.dto.SeatLockResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(
        name = "flight-service",
        fallbackFactory = FlightFeignClientFallbackFactory.class
)
public interface FlightFeignClient {

    @GetMapping("/api/flights/{flightId}/availability")
    FlightAvailabilityResponse checkAvailability(@PathVariable UUID flightId);

    @PostMapping("/api/flights/{flightId}/seats/lock")
    SeatLockResponse lockSeat(
            @PathVariable UUID flightId,
            @RequestBody SeatLockRequest request
    );


    @PostMapping("/api/flights/{flightId}/seats/release")
    void releaseSeat(
            @PathVariable UUID flightId,
            @RequestBody SeatLockRequest request
    );
}
