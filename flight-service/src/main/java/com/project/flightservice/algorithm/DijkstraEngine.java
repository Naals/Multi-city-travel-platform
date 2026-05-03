package com.project.flightservice.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Slf4j
@Component
public class DijkstraEngine {

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
        Map<UUID, double[]>    bestCost   = new HashMap<>();
        Map<UUID, GraphEdge[]> parentEdge = new HashMap<>();
        List<RouteResult>      results    = new ArrayList<>();

        GraphNode source = new GraphNode(originId, "", "", 0.0, 0, null, null);
        pq.offer(source);
        bestCost.put(originId, initCostArray(maxStops + 2));
        bestCost.get(originId)[0] = 0.0;
        parentEdge.put(originId, new GraphEdge[maxStops + 2]);

        log.debug("Dijkstra: {} → {} strategy={} maxStops={} topK={}",
                originId, destId, strategy, maxStops, topK);

        while (!pq.isEmpty() && results.size() < topK) {

            GraphNode current = pq.poll();

            if (current.getCityId().equals(destId)) {
                RouteResult route = reconstructPath(current, parentEdge, maxStops);
                if (route != null && !isDuplicate(results, route)) {
                    results.add(route);
                    log.debug("Dijkstra found path #{}: cost={} stops={}",
                            results.size(), route.getTotalCost(), route.getStopCount());
                }
                continue;
            }

            if (current.getStopCount() >= maxStops) continue;

            double[] costs = bestCost.getOrDefault(
                    current.getCityId(), initCostArray(maxStops + 2));
            if (current.getCumulativeCost() > costs[current.getStopCount()] + 1e-9) continue;

            List<GraphEdge> edges = graph.getOrDefault(
                    current.getCityId(), Collections.emptyList());

            for (GraphEdge edge : edges) {
                if (edge.getDepartureAt().isBefore(searchFrom)) continue;
                if (!edge.isReachableAfter(current.getArrivalAt())) continue;

                double newCost  = current.getCumulativeCost() + edge.getWeight(strategy);
                int    newStops = current.getStopCount() + 1;

                double[]    neighborCosts   = bestCost.computeIfAbsent(
                        edge.getDestCityId(), k -> initCostArray(maxStops + 2));
                GraphEdge[] neighborParents = parentEdge.computeIfAbsent(
                        edge.getDestCityId(), k -> new GraphEdge[maxStops + 2]);

                if (newCost < neighborCosts[newStops]) {
                    neighborCosts[newStops]   = newCost;
                    neighborParents[newStops] = edge;

                    pq.offer(new GraphNode(
                            edge.getDestCityId(),
                            edge.getDestCityCode(),
                            edge.getDestCityCode(),
                            newCost, newStops,
                            edge.getArrivalAt(),
                            edge
                    ));
                }
            }
        }

        log.debug("Dijkstra complete: {} paths found", results.size());
        return results;
    }

    private RouteResult reconstructPath(
            GraphNode destination,
            Map<UUID, GraphEdge[]> parentEdge,
            int maxStops) {

        List<GraphEdge> edges = new LinkedList<>();
        GraphNode current = destination;
        int safety = 0;

        while (current.getIncomingEdge() != null && safety < maxStops + 1) {
            GraphEdge incoming = current.getIncomingEdge();
            edges.add(0, incoming);

            UUID      parentCityId = incoming.getOriginCityId();
            int       parentStops  = current.getStopCount() - 1;
            GraphEdge[] parents    = parentEdge.get(parentCityId);

            if (parentStops < 0 || parents == null) break;

            GraphEdge parentIncoming = parentStops > 0 ? parents[parentStops] : null;
            current = new GraphNode(
                    parentCityId,
                    incoming.getOriginCityCode(),
                    incoming.getOriginCityCode(),
                    0.0, parentStops,
                    parentIncoming != null ? parentIncoming.getArrivalAt() : null,
                    parentIncoming
            );
            safety++;
        }

        if (edges.isEmpty()) return null;

        double totalPrice    = edges.stream()
                .mapToDouble(e -> e.getWeightPrice().doubleValue()).sum();
        int    totalDuration = edges.stream()
                .mapToInt(GraphEdge::getWeightDurationMin).sum();

        return RouteResult.builder()
                .edges(edges)
                .totalCost(destination.getCumulativeCost())
                .totalPrice(totalPrice)
                .totalDurationMinutes(totalDuration)
                .stopCount(edges.size() - 1)
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