package com.project.bookingservice.resilience;

import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Slf4j
@Configuration
public class ResilienceConfig {

    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> event) {
                CircuitBreaker cb = event.getAddedEntry();

                cb.getEventPublisher()
                        .onStateTransition(e -> log.warn(
                                "CircuitBreaker [{}] state: {} → {}",
                                cb.getName(),
                                e.getStateTransition().getFromState(),
                                e.getStateTransition().getToState()
                        ))
                        .onCallNotPermitted(e -> log.warn(
                                "CircuitBreaker [{}] OPEN — call rejected", cb.getName()
                        ))
                        .onError(e -> log.debug(
                                "CircuitBreaker [{}] recorded failure: {}",
                                cb.getName(), e.getThrowable().getMessage()
                        ));
            }

            @Override
            public void onEntryRemovedEvent(
                    io.github.resilience4j.core.registry.EntryRemovedEvent<CircuitBreaker> event) {}

            @Override
            public void onEntryReplacedEvent(
                    io.github.resilience4j.core.registry.EntryReplacedEvent<CircuitBreaker> event) {}
        };
    }
}
