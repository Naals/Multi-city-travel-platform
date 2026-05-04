package com.project.bookingservice.rabbitmq.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentRequestEvent {
    private String     eventType    = "PAYMENT_REQUEST";
    private String     bookingId;
    private String     userId;
    private BigDecimal amount;
    private String     currency;
    private String     flightNumber;
    private String     idempotencyKey;   // bookingId:attempt (prevents double charge)
    private Instant    requestedAt  = Instant.now();
}
