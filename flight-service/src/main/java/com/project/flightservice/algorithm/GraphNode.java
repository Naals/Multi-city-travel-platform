package com.project.flightservice.algorithm;

import lombok.*;

import java.util.UUID;

/**
 * GraphNode — a vertex in the Dijkstra flight graph.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GraphNode implements Comparable<GraphNode> {

    @EqualsAndHashCode.Include
    private final UUID   cityId;
    private final String cityName;
    private final String iataCityCode;

    private double cumulativeCost;

    private int stopCount;

    private java.time.Instant arrivalAt;

    private GraphEdge incomingEdge;

    public GraphNode(UUID cityId, String cityName, String iataCityCode) {
        this.cityId       = cityId;
        this.cityName     = cityName;
        this.iataCityCode = iataCityCode;
        this.cumulativeCost = 0.0;
        this.stopCount    = 0;
        this.arrivalAt    = null;
        this.incomingEdge = null;
    }

    @Override
    public int compareTo(GraphNode other) {
        return Double.compare(this.cumulativeCost, other.cumulativeCost);
    }
}
