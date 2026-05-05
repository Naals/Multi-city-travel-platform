package com.project.paymentservice.rabbitmq;

import com.project.paymentservice.rabbitmq.event.PaymentRequestEvent;
import com.project.paymentservice.service.PaymentService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRequestConsumer {

    private final PaymentService paymentService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_REQUEST_QUEUE, ackMode = "MANUAL")
    public void onPaymentRequest(
            PaymentRequestEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("Payment request received: bookingId={} amount={} idempotencyKey={}",
                event.getBookingId(), event.getAmount(), event.getIdempotencyKey());

        try {
            paymentService.processPayment(event);
            channel.basicAck(deliveryTag, false);

        } catch (Exception ex) {
            log.error("Payment processing failed for bookingId={}: {}",
                    event.getBookingId(), ex.getMessage());
            boolean requeue = shouldRequeue(event);
            channel.basicNack(deliveryTag, false, requeue);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_REFUND_QUEUE, ackMode = "MANUAL")
    public void onRefundRequest(
            PaymentRequestEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("Refund request received: bookingId={} amount={}",
                event.getBookingId(), event.getAmount());

        try {
            paymentService.processRefund(event);
            channel.basicAck(deliveryTag, false);

        } catch (Exception ex) {
            log.error("Refund processing failed for bookingId={}: {}",
                    event.getBookingId(), ex.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private boolean shouldRequeue(PaymentRequestEvent event) {
        return event.getIdempotencyKey() != null
                && !event.getIdempotencyKey().contains(":3");
    }
}