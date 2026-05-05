package com.project.paymentservice.controller;

import com.project.paymentservice.dto.PaymentDto;
import com.project.paymentservice.dto.RefundRequest;
import com.project.paymentservice.service.PaymentService;
import com.project.paymentservice.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment status and refund management")
public class PaymentController {

    private final PaymentService paymentService;
    private final RefundService  refundService;

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get payment by booking ID")
    public ResponseEntity<PaymentDto> getByBookingId(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(paymentService.getByBookingId(bookingId));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Issue a partial or full refund (admin only)")
    public ResponseEntity<Void> refund(
            @PathVariable UUID id,
            @Valid @RequestBody RefundRequest request) {
        refundService.issuePartialRefund(id, request.getAmount(), request.getReason());
        return ResponseEntity.noContent().build();
    }
}
