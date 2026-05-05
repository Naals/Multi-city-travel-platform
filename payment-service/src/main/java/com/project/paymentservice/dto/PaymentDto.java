package com.project.paymentservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class PaymentDto {
    private UUID          id;
    private UUID          bookingId;
    private UUID          userId;
    private String        idempotencyKey;
    private BigDecimal    amount;
    private String        currency;
    private String        method;
    private String        status;
    private String        gatewayTxId;
    private String        cardLastFour;
    private String        cardBrand;
    private BigDecimal    refundedAmount;
    private Instant       initiatedAt;
    private Instant       completedAt;
}