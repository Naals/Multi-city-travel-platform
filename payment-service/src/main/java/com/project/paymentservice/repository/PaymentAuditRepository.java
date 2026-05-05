package com.project.paymentservice.repository;

import com.project.paymentservice.model.PaymentAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentAuditRepository extends JpaRepository<PaymentAudit, Long> {
    List<PaymentAudit> findByPaymentIdOrderByOccurredAtAsc(UUID paymentId);
}