package com.project.reviewservice.controller;

import com.project.reviewservice.dto.*;
import com.project.reviewservice.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Flight reviews and ratings")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Submit a review (booking must be completed)")
    public ResponseEntity<ReviewDto> submit(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.submitReview(
                        UUID.fromString(userId), request));
    }

    @GetMapping("/flight/{flightId}")
    @Operation(summary = "Get all published reviews for a flight")
    public ResponseEntity<List<ReviewDto>> getFlightReviews(
            @PathVariable UUID flightId) {
        return ResponseEntity.ok(reviewService.getFlightReviews(flightId));
    }

    @GetMapping("/flight/{flightId}/summary")
    @Operation(summary = "Get aggregated rating summary for a flight")
    public ResponseEntity<FlightRatingSummary> getFlightSummary(
            @PathVariable UUID flightId) {
        return ResponseEntity.ok(reviewService.getFlightRatingSummary(flightId));
    }

    @GetMapping("/my")
    @Operation(summary = "Get current user's submitted reviews")
    public ResponseEntity<List<ReviewDto>> getMyReviews(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(
                reviewService.getMyReviews(UUID.fromString(userId)));
    }

    @PostMapping("/{id}/helpful")
    @Operation(summary = "Vote a review as helpful")
    public ResponseEntity<Void> voteHelpful(@PathVariable UUID id) {
        reviewService.voteHelpful(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/flag")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Flag a review for moderation (admin only)")
    public ResponseEntity<Void> flag(
            @PathVariable UUID id,
            @RequestParam String reason) {
        reviewService.flagReview(id, reason);
        return ResponseEntity.noContent().build();
    }
}