package com.project.paymentservice.kafka;

import com.project.paymentservice.kafka.event.PaymentCompletedEvent;
import com.project.paymentservice.kafka.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        send(KafkaConfig.TOPIC_PAYMENT_COMPLETED, event.getBookingId(), event);
        log.info("Published payment.completed: bookingId={} amount={}",
                event.getBookingId(), event.getAmount());
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        send(KafkaConfig.TOPIC_PAYMENT_FAILED, event.getBookingId(), event);
        log.warn("Published payment.failed: bookingId={} reason={}",
                event.getBookingId(), event.getFailureReason());
    }

    private void send(String topic, String key, Object payload) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, payload);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish to topic={} key={}: {}",
                        topic, key, ex.getMessage());
            } else {
                log.debug("Published to topic={} partition={} offset={}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}