package com.project.notificationservice.rabbitmq;

import com.project.notificationservice.model.NotificationEvent;
import com.project.notificationservice.model.NotificationType;
import com.project.notificationservice.service.NotificationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;

    private static final String DEDUP_PREFIX = "notif:dedup:";

    @RabbitListener(
            queues = RabbitMQConfig.NOTIF_BOOKING_QUEUE,
            ackMode = "MANUAL"
    )
    public void onBookingEvent(
            Map<String, Object> event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        String eventType = (String) event.get("eventType");
        String bookingId = (String) event.get("bookingId");
        String userId    = (String) event.get("userId");

        if (bookingId == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        String dedupKey = DEDUP_PREFIX + eventType + ":" + bookingId;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(dedupKey))) {
            log.debug("Duplicate booking event: {} bookingId={}", eventType, bookingId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            NotificationType type = resolveType(eventType);
            if (type == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            notificationService.dispatch(NotificationEvent.builder()
                    .userId(userId)
                    .type(type)
                    .subject(buildSubject(type, event))
                    .body(buildBody(type, event))
                    .metadata(event)
                    .occurredAt(Instant.now())
                    .build());

            redisTemplate.opsForValue().set(dedupKey, "1", Duration.ofHours(24));
            channel.basicAck(deliveryTag, false);

        } catch (Exception ex) {
            log.error("Failed to process booking event {}: {}", bookingId, ex.getMessage());
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private NotificationType resolveType(String eventType) {
        if (eventType == null) return null;
        return switch (eventType) {
            case "BOOKING_CONFIRMED" -> NotificationType.BOOKING_CONFIRMED;
            case "BOOKING_CANCELLED" -> NotificationType.BOOKING_CANCELLED;
            default -> null;
        };
    }

    private String buildSubject(NotificationType type, Map<String, Object> event) {
        String flightNumber = (String) event.getOrDefault("flightNumber", "your flight");
        return switch (type) {
            case BOOKING_CONFIRMED -> "Booking confirmed — " + flightNumber;
            case BOOKING_CANCELLED -> "Booking cancelled — " + flightNumber;
            default -> "Booking update";
        };
    }

    private String buildBody(NotificationType type, Map<String, Object> event) {
        String bookingId    = (String) event.get("bookingId");
        String flightNumber = (String) event.getOrDefault("flightNumber", "your flight");
        Object price        = event.get("pricePaid");
        return switch (type) {
            case BOOKING_CONFIRMED ->
                    String.format("Your booking for flight %s (ID: %s) is confirmed. " +
                                    "Amount charged: %s USD. See you at the gate!",
                            flightNumber, bookingId, price);
            case BOOKING_CANCELLED ->
                    String.format("Your booking %s for flight %s has been cancelled. " +
                                    "If eligible, a refund will be processed within 5-7 days.",
                            bookingId, flightNumber);
            default -> "Your booking has been updated.";
        };
    }
}