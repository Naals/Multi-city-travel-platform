package com.project.bookingservice.rabbitmq.event;

import com.project.bookingservice.rabbitmq.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentRequest(PaymentRequestEvent event) {
        event.setIdempotencyKey(event.getBookingId() + ":1");
        event.setRequestedAt(Instant.now());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENTS_EXCHANGE,
                RabbitMQConfig.RK_PAYMENT_REQUEST,
                event
        );
        log.info("Payment request published: bookingId={} amount={}{}",
                event.getBookingId(), event.getAmount(), event.getCurrency());
    }

    public void publishRefundRequest(String bookingId, BigDecimal amount) {
        var event = PaymentRequestEvent.builder()
                .eventType("REFUND_REQUEST")
                .bookingId(bookingId)
                .amount(amount)
                .idempotencyKey(bookingId + ":refund")
                .requestedAt(Instant.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENTS_EXCHANGE,
                RabbitMQConfig.RK_PAYMENT_REFUND,
                event
        );
        log.info("Refund request published: bookingId={} amount={}", bookingId, amount);
    }
}
