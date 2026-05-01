package com.project.flightservice.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Slf4j
@Component
public class DijkstraEngine {

    private static final int DEFAULT_MAX_STOPS = 3;
    private static final int DEFAULT_TOP_K     = 3;

    public List<RouteResult> search(
            Map<UUID, List<GraphEdge>> graph,
            UUID originId,
            UUID destId,
            SortStrategy strategy,
            int maxStops,
            int topK,
            Instant searchFrom) {

        if (originId.equals(destId)) {
            log.warn("Dijkstra: origin == destination ({})", originId);
            return Collections.emptyList();
        }

        PriorityQueue<GraphNode> pq = new PriorityQueue<>();

        Map<UUID, double[]> bestCost = new HashMap<>();

        List<RouteResult> results = new ArrayList<>();

        GraphNode source;
        source = new GraphNode(originId, "", "", 0.0, 0, null, null);
        pq.offer(source);
        bestCost.put(originId, initCostArray(maxStops + 1));
        bestCost.get(originId)[0] = 0.0;

        log.debug("Dijkstra start: {} → {} strategy={} maxStops={} topK={}",
                originId, destId, strategy, maxStops, topK);

        while (!pq.isEmpty() && results.size() < topK) {

            GraphNode current = pq.poll();

            if (current.getCityId().equals(destId)) {
                RouteResult route = reconstructPath(current);
                if (route != null && !isDuplicate(results, route)) {
                    results.add(route);
                    log.debug("Dijkstra found path #{}: cost={} stops={}",
                            results.size(), route.getTotalCost(), route.getStopCount());
                }
                continue;
            }

            if (current.getStopCount() >= maxStops) {
                continue;
            }

            double[] costs = bestCost.getOrDefault(
                    current.getCityId(), initCostArray(maxStops + 1)
            );
            if (current.getCumulativeCost() > costs[current.getStopCount()]) {
                continue;
            }

            List<GraphEdge> edges = graph.getOrDefault(
                    current.getCityId(), Collections.emptyList()
            );

            for (GraphEdge edge : edges) {

                if (edge.getDepartureAt().isBefore(searchFrom)) {
                    continue;
                }

                if (!edge.isReachableAfter(current.getArrivalAt())) {
                    continue;
                }

                double newCost = current.getCumulativeCost()
                        + edge.getWeight(strategy);
                int    newStops = current.getStopCount() + 1;

                double[] neighborCosts = bestCost.computeIfAbsent(
                        edge.getDestCityId(), k -> initCostArray(maxStops + 1)
                );

                if (newCost < neighborCosts[newStops]) {
                    neighborCosts[newStops] = newCost;

                    GraphNode neighbor = new GraphNode(
                            edge.getDestCityId(),
                            edge.getDestCityCode(),
                            edge.getDestCityCode(),
                            newCost,
                            newStops,
                            edge.getArrivalAt(),
                            edge
                    );
                    pq.offer(neighbor);
                }
            }
        }

        log.debug("Dijkstra complete: {} paths found for {} → {}",
                results.size(), originId, destId);
        return results;
    }

    private RouteResult reconstructPath(GraphNode destination) {
        List<GraphEdge> edges = new ArrayList<>();
        GraphNode current = destination;

        while (current.getIncomingEdge() != null) {
            edges.add(current.getIncomingEdge());

            current = new GraphNode(
                    current.getIncomingEdge().getOriginCityId(),
                    current.getIncomingEdge().getOriginCityCode(),
                    current.getIncomingEdge().getOriginCityCode(),
                    0.0, 0, null,
                    null
            );
            if (edges.size() > 10) break;
        }

        if (edges.isEmpty()) return null;

        Collections.reverse(edges);

        double totalPrice    = edges.stream()
                .mapToDouble(e -> e.getWeightPrice().doubleValue()).sum();
        int    totalDuration = edges.stream()
                .mapToInt(GraphEdge::getWeightDurationMin).sum();
        int    stopCount     = edges.size() - 1;

        return RouteResult.builder()
                .edges(edges)
                .totalCost(destination.getCumulativeCost())
                .totalPrice(totalPrice)
                .totalDurationMinutes(totalDuration)
                .stopCount(stopCount)
                .departureAt(edges.get(0).getDepartureAt())
                .arrivalAt(edges.get(edges.size() - 1).getArrivalAt())
                .build();
    }

    private boolean isDuplicate(List<RouteResult> results, RouteResult candidate) {
        return results.stream().anyMatch(r ->
                r.getEdges().stream().map(GraphEdge::getFlightId).toList()
                        .equals(candidate.getEdges().stream().map(GraphEdge::getFlightId).toList())
        );
    }

    private double[] initCostArray(int size) {
        double[] arr = new double[size];
        Arrays.fill(arr, Double.MAX_VALUE);
        return arr;
    }
}
