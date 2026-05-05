package com.project.paymentservice.repository;

import com.project.paymentservice.model.Payment;
import com.project.paymentservice.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByBookingId(UUID bookingId);

    List<Payment> findByUserIdOrderByInitiatedAtDesc(UUID userId);

    List<Payment> findByStatus(PaymentStatus status);

    @Modifying
    @Query("""
        UPDATE Payment p SET
            p.status = :status,
            p.completedAt = CURRENT_TIMESTAMP,
            p.gatewayTxId = :gatewayTxId,
            p.updatedAt = CURRENT_TIMESTAMP
        WHERE p.id = :id
    """)
    int markCompleted(
            @Param("id")           UUID id,
            @Param("status")       PaymentStatus status,
            @Param("gatewayTxId")  String gatewayTxId
    );

    @Modifying
    @Query("""
        UPDATE Payment p SET
            p.status = :status,
            p.refundedAmount = :refundedAmount,
            p.refundReason = :reason,
            p.refundedAt = CURRENT_TIMESTAMP,
            p.updatedAt = CURRENT_TIMESTAMP
        WHERE p.id = :id
    """)
    int markRefunded(
            @Param("id")             UUID id,
            @Param("status")         PaymentStatus status,
            @Param("refundedAmount") java.math.BigDecimal refundedAmount,
            @Param("reason")         String reason
    );
}