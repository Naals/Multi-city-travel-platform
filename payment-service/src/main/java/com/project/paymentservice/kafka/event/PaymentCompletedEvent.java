package com.project.paymentservice.kafka.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentCompletedEvent {
    private String     eventType   = "PAYMENT_COMPLETED";
    private String     paymentId;
    private String     bookingId;
    private String     userId;
    private BigDecimal amount;
    private String     currency;
    private String     gatewayTxId;
    private Instant    occurredAt  = Instant.now();
}