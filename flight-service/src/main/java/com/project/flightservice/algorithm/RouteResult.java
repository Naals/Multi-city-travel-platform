package com.project.flightservice.algorithm;

import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * RouteResult — output of one Dijkstra path.
 */
@Getter
@Builder
@AllArgsConstructor
public class RouteResult {

    private final List<GraphEdge> edges;
    private final double          totalCost;
    private final double          totalPrice;
    private final int             totalDurationMinutes;
    private final int             stopCount;
    private final Instant         departureAt;
    private final Instant         arrivalAt;

    public int getLegCount() {
        return edges != null ? edges.size() : 0;
    }
}
