package com.project.paymentservice.kafka.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentFailedEvent {
    private String     eventType     = "PAYMENT_FAILED";
    private String     paymentId;
    private String     bookingId;
    private String     userId;
    private BigDecimal amount;
    private String     failureReason;
    private Instant    occurredAt    = Instant.now();
}