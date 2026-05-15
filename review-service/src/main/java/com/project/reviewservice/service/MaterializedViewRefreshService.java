package com.project.reviewservice.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

@Slf4j
@Service
public class MaterializedViewRefreshService {

    @PersistenceContext
    private EntityManager entityManager;

    private final Set<UUID> pendingRefresh =
            ConcurrentHashMap.newKeySet();


    @Async
    public void scheduleRefresh(UUID flightId) {
        pendingRefresh.add(flightId);
        log.debug("Scheduled MV refresh for flightId={}", flightId);
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void refreshPendingViews() {
        if (pendingRefresh.isEmpty()) return;

        log.info("Refreshing mv_flight_ratings for {} flight(s)", pendingRefresh.size());
        try {
            entityManager.createNativeQuery(
                    "REFRESH MATERIALIZED VIEW CONCURRENTLY mv_flight_ratings"
            ).executeUpdate();

            pendingRefresh.clear();
            log.info("mv_flight_ratings refreshed successfully");

        } catch (Exception ex) {
            log.error("Failed to refresh mv_flight_ratings: {}", ex.getMessage());
        }
    }
}