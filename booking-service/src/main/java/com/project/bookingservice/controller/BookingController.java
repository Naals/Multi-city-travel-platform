package com.project.bookingservice.controller;

import com.project.bookingservice.dto.BookingRequest;
import com.project.bookingservice.dto.BookingResponse;
import com.project.bookingservice.model.CancellationReason;
import com.project.bookingservice.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Flight booking lifecycle management")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create a booking — validates user (gRPC), locks seat (Feign), queues payment (RabbitMQ)")
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(UUID.fromString(userId), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID (owner only)")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(
                bookingService.getBooking(id, UUID.fromString(userId))
        );
    }

    @GetMapping("/my")
    @Operation(summary = "Get all bookings for current user")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(
                bookingService.getUserBookings(UUID.fromString(userId))
        );
    }

    @GetMapping("/trip/{tripId}")
    @Operation(summary = "Get all flight legs of a multi-city trip")
    public ResponseEntity<List<BookingResponse>> getTripBookings(
            @PathVariable UUID tripId,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(bookingService.getTripBookings(tripId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel a booking (releases seat, triggers refund if confirmed)")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false, defaultValue = "USER_REQUESTED")
            CancellationReason reason,
            @RequestParam(required = false, defaultValue = "Cancelled by user")
            String note) {
        bookingService.cancelBooking(
                id, UUID.fromString(userId), reason, note
        );
        return ResponseEntity.noContent().build();
    }
}
