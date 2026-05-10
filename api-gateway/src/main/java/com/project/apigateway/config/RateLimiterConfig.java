package com.project.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;


@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public RedisRateLimiter ipRateLimiter() {
        return new RedisRateLimiter(50, 100, 1);
    }


    @Bean
    public RedisRateLimiter userRateLimiter() {
        return new RedisRateLimiter(100, 200, 1);
    }


    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            var forwarded = exchange.getRequest()
                    .getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return Mono.just(forwarded.split(",")[0].trim());
            }
            var address = exchange.getRequest().getRemoteAddress();
            return Mono.just(address != null
                    ? address.getAddress().getHostAddress()
                    : "unknown");
        };
    }


    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest()
                    .getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            var address = exchange.getRequest().getRemoteAddress();
            return Mono.just("ip:" + (address != null
                    ? address.getAddress().getHostAddress()
                    : "unknown"));
        };
    }
}