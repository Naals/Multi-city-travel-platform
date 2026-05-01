package com.project.flightservice.controller;

import com.project.flightservice.model.Flight;
import com.project.flightservice.service.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
@Tag(name = "Flights", description = "Flight management and status updates")
public class FlightController {

    private final FlightService flightService;

    @PatchMapping("/{id}/delay")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update flight delay (triggers Kafka event)")
    public ResponseEntity<Void> delayFlight(
            @PathVariable UUID id,
            @RequestParam @Min(1) @Max(1440) int delayMinutes,
            @RequestParam @NotBlank String reason) {
        flightService.updateFlightDelay(id, delayMinutes, reason);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel a flight (triggers Kafka event + graph invalidation)")
    public ResponseEntity<Void> cancelFlight(
            @PathVariable UUID id,
            @RequestParam @NotBlank String reason) {
        flightService.cancelFlight(id, reason);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get flight by ID")
    public ResponseEntity<Flight> getFlight(@PathVariable UUID id) {
        return ResponseEntity.ok(flightService.findOrThrow(id));
    }
}
