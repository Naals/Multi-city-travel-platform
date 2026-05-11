package com.project.notificationservice.kafka;

import com.project.notificationservice.model.NotificationEvent;
import com.project.notificationservice.model.NotificationType;
import com.project.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlightEventConsumer {

    private final NotificationService  notificationService;
    private final StringRedisTemplate  redisTemplate;

    private static final String DEDUP_PREFIX = "notif:dedup:";

    @KafkaListener(
            topics = "flight.delayed",
            groupId = "notification-service"
    )
    public void onFlightDelayed(Map<String, Object> event, Acknowledgment ack) {
        String flightId     = (String) event.get("flightId");
        String flightNumber = (String) event.get("flightNumber");
        Object delayMins    = event.get("delayMinutes");
        String newDeparture = String.valueOf(event.get("newDeparture"));

        String dedupKey = DEDUP_PREFIX + "FLIGHT_DELAYED:" + flightId;

        if (isDuplicate(dedupKey)) {
            log.debug("Duplicate flight.delayed event for flightId={}", flightId);
            ack.acknowledge();
            return;
        }

        log.info("flight.delayed received: flightId={} delay={}min",
                flightId, delayMins);

        notificationService.dispatch(NotificationEvent.builder()
                .type(NotificationType.FLIGHT_DELAYED)
                .subject("Flight " + flightNumber + " is delayed")
                .body(String.format(
                        "Your flight %s has been delayed by %s minutes. " +
                                "New departure time: %s. We apologize for the inconvenience.",
                        flightNumber, delayMins, newDeparture))
                .metadata(event)
                .occurredAt(Instant.now())
                .build());

        markSent(dedupKey);
        ack.acknowledge();
    }

    @KafkaListener(
            topics = "flight.cancelled",
            groupId = "notification-service"
    )
    public void onFlightCancelled(Map<String, Object> event, Acknowledgment ack) {
        String flightId     = (String) event.get("flightId");
        String flightNumber = (String) event.get("flightNumber");
        String reason       = (String) event.getOrDefault(
                "cancellationReason", "operational reasons");

        String dedupKey = DEDUP_PREFIX + "FLIGHT_CANCELLED:" + flightId;

        if (isDuplicate(dedupKey)) {
            log.debug("Duplicate flight.cancelled for flightId={}", flightId);
            ack.acknowledge();
            return;
        }

        log.info("flight.cancelled received: flightId={}", flightId);

        notificationService.dispatch(NotificationEvent.builder()
                .type(NotificationType.FLIGHT_CANCELLED)
                .subject("Flight " + flightNumber + " has been cancelled")
                .body(String.format(
                        "We regret to inform you that flight %s has been cancelled due to %s. " +
                                "A full refund will be processed to your original payment method " +
                                "within 5-7 business days.",
                        flightNumber, reason))
                .metadata(event)
                .occurredAt(Instant.now())
                .build());

        markSent(dedupKey);
        ack.acknowledge();
    }

    @KafkaListener(
            topics = "payment.completed",
            groupId = "notification-service"
    )
    public void onPaymentCompleted(Map<String, Object> event, Acknowledgment ack) {
        String bookingId = (String) event.get("bookingId");
        String userId    = (String) event.get("userId");
        Object amount    = event.get("amount");
        String currency  = (String) event.getOrDefault("currency", "USD");

        String dedupKey = DEDUP_PREFIX + "PAYMENT_COMPLETED:" + bookingId;

        if (isDuplicate(dedupKey)) {
            ack.acknowledge();
            return;
        }

        notificationService.dispatch(NotificationEvent.builder()
                .userId(userId)
                .type(NotificationType.PAYMENT_RECEIVED)
                .subject("Booking confirmed — payment received")
                .body(String.format(
                        "Your payment of %s %s has been received. " +
                                "Your booking ID is %s. Have a great trip!",
                        amount, currency, bookingId))
                .metadata(event)
                .occurredAt(Instant.now())
                .build());

        markSent(dedupKey);
        ack.acknowledge();
    }

    @KafkaListener(
            topics = "payment.failed",
            groupId = "notification-service"
    )
    public void onPaymentFailed(Map<String, Object> event, Acknowledgment ack) {
        String bookingId = (String) event.get("bookingId");
        String userId    = (String) event.get("userId");
        String reason    = (String) event.getOrDefault("failureReason", "unknown");

        String dedupKey = DEDUP_PREFIX + "PAYMENT_FAILED:" + bookingId;

        if (isDuplicate(dedupKey)) {
            ack.acknowledge();
            return;
        }

        notificationService.dispatch(NotificationEvent.builder()
                .userId(userId)
                .type(NotificationType.PAYMENT_FAILED)
                .subject("Payment failed for your booking")
                .body(String.format(
                        "Unfortunately, your payment for booking %s could not be processed. " +
                                "Reason: %s. Please try again or use a different payment method.",
                        bookingId, reason))
                .metadata(event)
                .occurredAt(Instant.now())
                .build());

        markSent(dedupKey);
        ack.acknowledge();
    }

    private boolean isDuplicate(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private void markSent(String key) {
        redisTemplate.opsForValue().set(key, "1", Duration.ofHours(24));
    }
}