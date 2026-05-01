package com.project.flightservice.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.flightservice.algorithm.GraphEdge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper                  objectMapper;

    private static final String GRAPH_KEY = "flight:graph:v1";

    @Value("${graph.cache.ttl-minutes:5}")
    private long cacheTtlMinutes;

    public void storeGraph(Map<UUID, List<GraphEdge>> graph) {
        try {
            Map<String, List<GraphEdge>> serializable = new LinkedHashMap<>();
            graph.forEach((k, v) -> serializable.put(k.toString(), v));

            String json = objectMapper.writeValueAsString(serializable);
            redisTemplate.opsForValue().set(
                    GRAPH_KEY, json, Duration.ofMinutes(cacheTtlMinutes)
            );
            log.debug("Graph stored in Redis: {} city nodes, TTL={}min",
                    graph.size(), cacheTtlMinutes);
        } catch (Exception e) {
            log.error("Failed to store graph in Redis — searches will use DB fallback", e);
        }
    }

    public Map<UUID, List<GraphEdge>> loadGraph() {
        try {
            String json = redisTemplate.opsForValue().get(GRAPH_KEY);
            if (json == null) {
                log.debug("Graph cache miss");
                return null;
            }

            Map<String, List<GraphEdge>> raw = objectMapper.readValue(
                    json,
                    new TypeReference<>() {
                    }
            );

            Map<UUID, List<GraphEdge>> graph = new LinkedHashMap<>();
            raw.forEach((k, v) -> graph.put(UUID.fromString(k), v));

            log.debug("Graph loaded from Redis: {} city nodes", graph.size());
            return graph;

        } catch (Exception e) {
            log.error("Failed to load graph from Redis — forcing rebuild", e);
            return null;
        }
    }

    public void invalidate() {
        redisTemplate.delete(GRAPH_KEY);
        log.info("Flight graph cache invalidated");
    }

    public boolean isCached() {
        return Boolean.TRUE.equals(redisTemplate.hasKey(GRAPH_KEY));
    }
}
