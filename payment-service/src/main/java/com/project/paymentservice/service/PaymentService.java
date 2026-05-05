package com.project.paymentservice.service;

import com.project.paymentservice.dto.PaymentDto;
import com.project.paymentservice.exception.*;
import com.project.paymentservice.kafka.PaymentEventProducer;
import com.project.paymentservice.kafka.event.PaymentCompletedEvent;
import com.project.paymentservice.kafka.event.PaymentFailedEvent;
import com.project.paymentservice.model.*;
import com.project.paymentservice.rabbitmq.event.PaymentRequestEvent;
import com.project.paymentservice.repository.*;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository      paymentRepo;
    private final PaymentAuditRepository auditRepo;
    private final PaymentEventProducer   eventProducer;
    private final StringRedisTemplate    redisTemplate;

    private static final String IDEMPOTENCY_PREFIX = "payment:idem:";

    @Bulkhead(name = "paymentProcessing")
    @Transactional
    public void processPayment(PaymentRequestEvent event) {
        String idempotencyKey = event.getIdempotencyKey();

        String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            log.info("Duplicate payment request (Redis): key={}", idempotencyKey);
            return;
        }

        if (paymentRepo.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.info("Duplicate payment request (DB): key={}", idempotencyKey);
            redisTemplate.opsForValue().set(redisKey, "1", Duration.ofHours(24));
            return;
        }

        Payment payment = Payment.builder()
                .bookingId(UUID.fromString(event.getBookingId()))
                .userId(UUID.fromString(event.getUserId()))
                .idempotencyKey(idempotencyKey)
                .amount(event.getAmount())
                .currency(event.getCurrency() != null ? event.getCurrency() : "USD")
                .method(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .gatewayName("INTERNAL_SIMULATOR")
                .build();

        try {
            payment = paymentRepo.save(payment);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Payment already exists (race condition): key={}", idempotencyKey);
            return;
        }

        audit(payment.getId(), null, PaymentStatus.PENDING, event.getAmount(),
                "Payment initiated");

        Payment processed = simulateGateway(payment);

        if (processed.getStatus() == PaymentStatus.COMPLETED) {
            redisTemplate.opsForValue().set(redisKey, "1", Duration.ofHours(24));

            eventProducer.publishPaymentCompleted(PaymentCompletedEvent.builder()
                    .paymentId(processed.getId().toString())
                    .bookingId(event.getBookingId())
                    .userId(event.getUserId())
                    .amount(processed.getAmount())
                    .currency(processed.getCurrency())
                    .gatewayTxId(processed.getGatewayTxId())
                    .build());

            log.info("Payment completed: id={} bookingId={}",
                    processed.getId(), event.getBookingId());

        } else {
            eventProducer.publishPaymentFailed(PaymentFailedEvent.builder()
                    .paymentId(processed.getId().toString())
                    .bookingId(event.getBookingId())
                    .userId(event.getUserId())
                    .amount(processed.getAmount())
                    .failureReason(processed.getGatewayTxId())
                    .build());

            log.warn("Payment failed: id={} bookingId={}",
                    processed.getId(), event.getBookingId());
        }
    }


    @Transactional
    public void processRefund(PaymentRequestEvent event) {
        Payment payment = paymentRepo.findByBookingId(
                        UUID.fromString(event.getBookingId()))
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No payment found for bookingId: " + event.getBookingId()));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.info("Refund already processed for bookingId={}", event.getBookingId());
            return;
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RefundNotAllowedException(
                    "Cannot refund payment in status: " + payment.getStatus());
        }

        PaymentStatus prevStatus = payment.getStatus();

        BigDecimal refundAmount = event.getAmount() != null
                ? event.getAmount()
                : payment.getAmount();

        paymentRepo.markRefunded(
                payment.getId(),
                PaymentStatus.REFUNDED,
                refundAmount,
                "Booking cancelled"
        );

        audit(payment.getId(), prevStatus, PaymentStatus.REFUNDED,
                refundAmount, "Refund processed");

        eventProducer.publishPaymentCompleted(PaymentCompletedEvent.builder()
                .paymentId(payment.getId().toString())
                .bookingId(event.getBookingId())
                .userId(payment.getUserId().toString())
                .amount(refundAmount)
                .currency(payment.getCurrency())
                .gatewayTxId("REFUND-" + payment.getGatewayTxId())
                .build());

        log.info("Refund completed: paymentId={} bookingId={} amount={}",
                payment.getId(), event.getBookingId(), refundAmount);
    }


    @Transactional(readOnly = true)
    public PaymentDto getByBookingId(UUID bookingId) {
        Payment payment = paymentRepo.findByBookingId(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for booking: " + bookingId));
        return toDto(payment);
    }

    @Transactional(readOnly = true)
    public PaymentDto getById(UUID paymentId) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found: " + paymentId));
        return toDto(payment);
    }


    private Payment simulateGateway(Payment payment) {
        boolean success = Math.random() > 0.05;
        String txId = success
                ? "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                : "ERR-INSUFFICIENT_FUNDS";

        PaymentStatus newStatus = success
                ? PaymentStatus.COMPLETED
                : PaymentStatus.FAILED;

        paymentRepo.markCompleted(payment.getId(), newStatus, txId);

        payment.setStatus(newStatus);
        payment.setGatewayTxId(txId);
        if (success) payment.setCompletedAt(Instant.now());

        audit(payment.getId(), PaymentStatus.PENDING, newStatus,
                payment.getAmount(),
                success ? "Gateway approved: " + txId : "Gateway declined: " + txId);

        return payment;
    }


    private void audit(UUID paymentId, PaymentStatus from,
                       PaymentStatus to, BigDecimal amount, String note) {
        auditRepo.save(PaymentAudit.builder()
                .paymentId(paymentId)
                .fromStatus(from)
                .toStatus(to)
                .amount(amount)
                .note(note)
                .occurredAt(Instant.now())
                .build());
    }

    private PaymentDto toDto(Payment p) {
        return PaymentDto.builder()
                .id(p.getId())
                .bookingId(p.getBookingId())
                .userId(p.getUserId())
                .idempotencyKey(p.getIdempotencyKey())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .method(p.getMethod() != null ? p.getMethod().name() : null)
                .status(p.getStatus().name())
                .gatewayTxId(p.getGatewayTxId())
                .cardLastFour(p.getCardLastFour())
                .cardBrand(p.getCardBrand())
                .refundedAmount(p.getRefundedAmount())
                .initiatedAt(p.getInitiatedAt())
                .completedAt(p.getCompletedAt())
                .build();
    }
}