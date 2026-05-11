package com.project.notificationservice.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;


@Slf4j
@Component
@RequiredArgsConstructor
public class EventDeduplicator {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX   = "notif:dedup:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    public boolean isDuplicate(String eventType, String entityId) {
        String key = buildKey(eventType, entityId);
        boolean duplicate = Boolean.TRUE.equals(redisTemplate.hasKey(key));
        if (duplicate) {
            log.debug("Duplicate event suppressed: type={} id={}", eventType, entityId);
        }
        return duplicate;
    }

    public void markProcessed(String eventType, String entityId) {
        redisTemplate.opsForValue()
                .set(buildKey(eventType, entityId), "1", DEFAULT_TTL);
    }

    public void markProcessed(String eventType, String entityId, Duration ttl) {
        redisTemplate.opsForValue()
                .set(buildKey(eventType, entityId), "1", ttl);
    }

    private String buildKey(String eventType, String entityId) {
        return PREFIX + eventType + ":" + entityId;
    }
}