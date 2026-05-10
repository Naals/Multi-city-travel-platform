package com.project.apigateway.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
public class FallbackController {

    @RequestMapping("/fallback/auth-service")
    public Mono<ResponseEntity<Map<String, Object>>> authFallback(
            ServerWebExchange exchange) {
        return fallback("auth-service",
                "Authentication service is temporarily unavailable. " +
                        "Please try again in a moment.");
    }

    @RequestMapping("/fallback/user-service")
    public Mono<ResponseEntity<Map<String, Object>>> userFallback(
            ServerWebExchange exchange) {
        return fallback("user-service",
                "User service is temporarily unavailable. " +
                        "Please try again in a moment.");
    }

    @RequestMapping("/fallback/flight-service")
    public Mono<ResponseEntity<Map<String, Object>>> flightFallback(
            ServerWebExchange exchange) {
        return fallback("flight-service",
                "Flight search is temporarily unavailable. " +
                        "Please try again in a moment.");
    }

    @RequestMapping("/fallback/booking-service")
    public Mono<ResponseEntity<Map<String, Object>>> bookingFallback(
            ServerWebExchange exchange) {
        return fallback("booking-service",
                "Booking service is temporarily unavailable. " +
                        "Your booking has NOT been created. Please try again.");
    }

    @RequestMapping("/fallback/payment-service")
    public Mono<ResponseEntity<Map<String, Object>>> paymentFallback(
            ServerWebExchange exchange) {
        return fallback("payment-service",
                "Payment service is temporarily unavailable. " +
                        "No charge has been made. Please try again.");
    }

    @RequestMapping("/fallback/review-service")
    public Mono<ResponseEntity<Map<String, Object>>> reviewFallback(
            ServerWebExchange exchange) {
        return fallback("review-service",
                "Review service is temporarily unavailable. " +
                        "Please try again in a moment.");
    }

    private Mono<ResponseEntity<Map<String, Object>>> fallback(
            String service, String message) {
        log.warn("Circuit breaker fallback triggered for service: {}", service);
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status",    503,
                        "error",     "Service Unavailable",
                        "service",   service,
                        "message",   message,
                        "timestamp", Instant.now().toString()
                )));
    }
}
