package com.project.paymentservice.service;

import com.project.paymentservice.exception.*;
import com.project.paymentservice.model.*;
import com.project.paymentservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final PaymentRepository      paymentRepo;
    private final PaymentAuditRepository auditRepo;

    @Transactional
    public void issuePartialRefund(UUID paymentId, BigDecimal amount, String reason) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RefundNotAllowedException(
                    "Only COMPLETED payments can be refunded. Current status: "
                            + payment.getStatus());
        }

        BigDecimal alreadyRefunded = payment.getRefundedAmount() != null
                ? payment.getRefundedAmount() : BigDecimal.ZERO;
        BigDecimal maxRefundable = payment.getAmount().subtract(alreadyRefunded);

        if (amount.compareTo(maxRefundable) > 0) {
            throw new RefundNotAllowedException(
                    String.format("Refund amount %.2f exceeds max refundable %.2f",
                            amount, maxRefundable));
        }

        BigDecimal totalRefunded = alreadyRefunded.add(amount);
        boolean fullyRefunded = totalRefunded.compareTo(payment.getAmount()) >= 0;

        PaymentStatus newStatus = fullyRefunded
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;

        paymentRepo.markRefunded(paymentId, newStatus, totalRefunded, reason);

        auditRepo.save(PaymentAudit.builder()
                .paymentId(paymentId)
                .fromStatus(PaymentStatus.COMPLETED)
                .toStatus(newStatus)
                .amount(amount)
                .note("Partial refund: " + reason)
                .occurredAt(java.time.Instant.now())
                .build());

        log.info("Partial refund issued: paymentId={} amount={} newStatus={}",
                paymentId, amount, newStatus);
    }

    @Transactional(readOnly = true)
    public List<PaymentAudit> getAuditTrail(UUID paymentId) {
        return auditRepo.findByPaymentIdOrderByOccurredAtAsc(paymentId);
    }
}