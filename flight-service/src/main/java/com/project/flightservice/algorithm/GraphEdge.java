package com.project.flightservice.algorithm;

import com.project.flightservice.model.CabinClass;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * GraphEdge — a directed edge between two city nodes.
 */
@Getter
@AllArgsConstructor
@Builder
public class GraphEdge {

    private final UUID       flightRouteId;
    private final UUID       flightId;
    private final UUID       originCityId;
    private final UUID       destCityId;
    private final String     originCityCode;
    private final String     destCityCode;

    private final BigDecimal weightPrice;
    private final int        weightDurationMin;

    private final int        minConnectionMin;

    private final Instant    departureAt;
    private final Instant    arrivalAt;

    private final String     airlineCode;
    private final String     airlineName;
    private final CabinClass cabin;
    private final BigDecimal currentPrice;

    public double getWeight(SortStrategy strategy) {
        return switch (strategy) {
            case PRICE    -> weightPrice.doubleValue();
            case DURATION -> (double) weightDurationMin;
            case STOPS    -> 1.0;
        };
    }

    public boolean isReachableAfter(Instant previousArrival) {
        if (previousArrival == null) return true;
        long gapMinutes = java.time.Duration
                .between(previousArrival, departureAt)
                .toMinutes();
        return gapMinutes >= minConnectionMin;
    }
}
