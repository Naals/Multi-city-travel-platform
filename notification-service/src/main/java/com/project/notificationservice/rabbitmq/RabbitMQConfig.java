package com.project.notificationservice.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String BOOKINGS_EXCHANGE        = "travel.bookings";
    public static final String PAYMENTS_EXCHANGE        = "travel.payments";
    public static final String NOTIF_BOOKING_QUEUE      = "notification.booking.events";
    public static final String NOTIF_PAYMENT_QUEUE      = "notification.payment.events";

    @Bean
    public TopicExchange bookingsExchange() {
        return ExchangeBuilder.topicExchange(BOOKINGS_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange paymentsExchange() {
        return ExchangeBuilder.directExchange(PAYMENTS_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue notifBookingQueue() {
        return QueueBuilder.durable(NOTIF_BOOKING_QUEUE).build();
    }

    @Bean
    public Queue notifPaymentQueue() {
        return QueueBuilder.durable(NOTIF_PAYMENT_QUEUE).build();
    }

    @Bean
    public Binding notifBookingBinding() {
        return BindingBuilder.bind(notifBookingQueue())
                .to(bookingsExchange()).with("booking.*");
    }

    @Bean
    public Binding notifPaymentBinding() {
        return BindingBuilder.bind(notifPaymentQueue())
                .to(paymentsExchange()).with("payment.refund");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}