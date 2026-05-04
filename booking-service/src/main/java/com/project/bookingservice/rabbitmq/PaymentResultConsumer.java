package com.project.bookingservice.rabbitmq;

import com.project.bookingservice.service.BookingService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResultConsumer {

    private final BookingService bookingService;

    @RabbitListener(
            queues = "booking.payment.completed",
            ackMode = "MANUAL"
    )
    public void onPaymentCompleted(
            Map<String, Object> message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        String bookingId = (String) message.get("bookingId");
        log.info("Payment completed received for bookingId={}", bookingId);

        try {
            bookingService.confirmBooking(UUID.fromString(bookingId));
            channel.basicAck(deliveryTag, false);
            log.info("Booking confirmed: {}", bookingId);

        } catch (Exception ex) {
            log.error("Failed to confirm booking {}: {}", bookingId, ex.getMessage());
            boolean requeue = shouldRequeue(message);
            channel.basicNack(deliveryTag, false, requeue);
        }
    }

    @RabbitListener(
            queues = "booking.payment.failed",
            ackMode = "MANUAL"
    )
    public void onPaymentFailed(
            Map<String, Object> message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        String bookingId = (String) message.get("bookingId");
        String reason    = (String) message.getOrDefault("failureReason", "PAYMENT_FAILED");
        log.warn("Payment failed for bookingId={} reason={}", bookingId, reason);

        try {
            bookingService.cancelBooking(
                    UUID.fromString(bookingId),
                    extractUserId(message),
                    com.project.bookingservice.model.CancellationReason.PAYMENT_FAILED,
                    "Payment failed: " + reason
            );
            channel.basicAck(deliveryTag, false);

        } catch (Exception ex) {
            log.error("Failed to cancel booking after payment failure {}: {}",
                    bookingId, ex.getMessage());
            channel.basicNack(deliveryTag, false, false);   // send to DLQ
        }
    }

    private boolean shouldRequeue(Map<String, Object> message) {
        Integer retryCount = (Integer) message.getOrDefault("retryCount", 0);
        return retryCount < 3;
    }

    private UUID extractUserId(Map<String, Object> message) {
        Object uid = message.get("userId");
        return uid != null ? UUID.fromString(uid.toString()) : UUID.randomUUID();
    }
}
