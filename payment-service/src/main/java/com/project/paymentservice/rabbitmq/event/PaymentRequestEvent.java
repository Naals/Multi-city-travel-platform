package com.project.paymentservice.rabbitmq.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentRequestEvent {
    private String     eventType;
    private String     bookingId;
    private String     userId;
    private BigDecimal amount;
    private String     currency;
    private String     flightNumber;
    private String     idempotencyKey;
    private Instant    requestedAt;
}