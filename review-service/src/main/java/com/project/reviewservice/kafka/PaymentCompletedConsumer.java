package com.project.reviewservice.kafka;

import com.project.reviewservice.model.ReviewEligibility;
import com.project.reviewservice.repository.ReviewEligibilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedConsumer {

    private final ReviewEligibilityRepository eligibilityRepo;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_PAYMENT_COMPLETED,
            groupId = "review-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentCompleted(Map<String, Object> event, Acknowledgment ack) {
        String bookingId = (String) event.get("bookingId");
        String userId    = (String) event.get("userId");
        String flightId  = (String) event.getOrDefault("flightId", null);
        String tripId    = (String) event.getOrDefault("tripId", null);

        if (bookingId == null || userId == null) {
            log.warn("Received payment.completed with missing fields: {}", event);
            ack.acknowledge();
            return;
        }

        log.info("payment.completed received — unlocking review: bookingId={}", bookingId);

        try {
            ReviewEligibility eligibility = ReviewEligibility.builder()
                    .userId(UUID.fromString(userId))
                    .flightId(flightId != null
                            ? UUID.fromString(flightId)
                            : UUID.randomUUID())
                    .bookingId(UUID.fromString(bookingId))
                    .tripId(tripId != null
                            ? UUID.fromString(tripId)
                            : UUID.randomUUID())
                    .eligibleAt(Instant.now())
                    .reviewed(false)
                    .build();

            eligibilityRepo.save(eligibility);
            log.info("Review eligibility unlocked: userId={} bookingId={}",
                    userId, bookingId);

        } catch (DataIntegrityViolationException ex) {
            log.debug("Review eligibility already exists for bookingId={}", bookingId);
        } catch (Exception ex) {
            log.error("Failed to unlock review eligibility bookingId={}: {}",
                    bookingId, ex.getMessage());
            return;
        }

        ack.acknowledge();
    }
}
