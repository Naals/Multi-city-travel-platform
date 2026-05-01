package com.project.flightservice.kafka;

import com.project.flightservice.kafka.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String TOPIC_FLIGHT_DELAYED   = "flight.delayed";
    public static final String TOPIC_FLIGHT_CANCELLED = "flight.cancelled";
    public static final String TOPIC_FLIGHT_UPDATED   = "flight.updated";

    public void publishFlightDelayed(FlightDelayedEvent event) {
        sendWithCallback(TOPIC_FLIGHT_DELAYED, event.getFlightId(), event);
        log.info("Published flight.delayed for flightId={} delay={}min",
                event.getFlightId(), event.getDelayMinutes());
    }

    public void publishFlightCancelled(FlightCancelledEvent event) {
        sendWithCallback(TOPIC_FLIGHT_CANCELLED, event.getFlightId(), event);
        log.info("Published flight.cancelled for flightId={}",
                event.getFlightId());
    }

    public void publishFlightUpdated(FlightUpdatedEvent event) {
        sendWithCallback(TOPIC_FLIGHT_UPDATED, event.getFlightId(), event);
    }

    private void sendWithCallback(String topic, String key, Object payload) {
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
