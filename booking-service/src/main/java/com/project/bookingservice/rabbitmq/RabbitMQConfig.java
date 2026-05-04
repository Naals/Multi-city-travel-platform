package com.project.bookingservice.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYMENTS_EXCHANGE   = "travel.payments";
    public static final String BOOKINGS_EXCHANGE   = "travel.bookings";
    public static final String DLX_EXCHANGE        = "travel.dlx";

    public static final String PAYMENT_REQUEST_QUEUE   = "payment.requests";
    public static final String PAYMENT_REFUND_QUEUE    = "payment.refunds";
    public static final String BOOKING_EVENTS_QUEUE    = "booking.events";
    public static final String PAYMENT_REQUEST_DLQ     = "payment.requests.dlq";

    public static final String RK_PAYMENT_REQUEST  = "payment.request";
    public static final String RK_PAYMENT_REFUND   = "payment.refund";
    public static final String RK_BOOKING_CONFIRMED = "booking.confirmed";
    public static final String RK_BOOKING_CANCELLED = "booking.cancelled";


    @Bean
    public DirectExchange paymentsExchange() {
        return ExchangeBuilder.directExchange(PAYMENTS_EXCHANGE)
                .durable(true).build();
    }

    @Bean
    public TopicExchange bookingsExchange() {
        return ExchangeBuilder.topicExchange(BOOKINGS_EXCHANGE)
                .durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE)
                .durable(true).build();
    }

    @Bean
    public Queue paymentRequestQueue() {
        return QueueBuilder.durable(PAYMENT_REQUEST_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "payment.request.dead")
                .withArgument("x-message-ttl", 86_400_000)  // 24h TTL
                .build();
    }

    @Bean
    public Queue paymentRefundQueue() {
        return QueueBuilder.durable(PAYMENT_REFUND_QUEUE).build();
    }

    @Bean
    public Queue bookingEventsQueue() {
        return QueueBuilder.durable(BOOKING_EVENTS_QUEUE).build();
    }

    @Bean
    public Queue paymentRequestDlq() {
        return QueueBuilder.durable(PAYMENT_REQUEST_DLQ).build();
    }

    @Bean
    public Binding paymentRequestBinding() {
        return BindingBuilder.bind(paymentRequestQueue())
                .to(paymentsExchange()).with(RK_PAYMENT_REQUEST);
    }

    @Bean
    public Binding paymentRefundBinding() {
        return BindingBuilder.bind(paymentRefundQueue())
                .to(paymentsExchange()).with(RK_PAYMENT_REFUND);
    }

    @Bean
    public Binding bookingEventsBinding() {
        return BindingBuilder.bind(bookingEventsQueue())
                .to(bookingsExchange()).with("booking.*");
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(paymentRequestDlq())
                .to(deadLetterExchange()).with("payment.request.dead");
    }


    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(messageConverter());
        template.setConfirmCallback((correlation, ack, reason) -> {
            if (!ack) {
                System.err.println("RabbitMQ publish NACK: " + reason);
            }
        });
        return template;
    }
}
