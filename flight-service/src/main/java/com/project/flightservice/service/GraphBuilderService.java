package com.project.flightservice.service;

import com.project.flightservice.algorithm.GraphEdge;
import com.project.flightservice.model.FlightRoute;
import com.project.flightservice.repository.FlightRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphBuilderService {

    private final FlightRouteRepository  routeRepository;
    private final GraphCacheService      cacheService;


    @Transactional(readOnly = true)
    public Map<UUID, List<GraphEdge>> buildAndCacheGraph() {
        log.info("Building flight graph from DB...");
        Instant now = Instant.now();

        List<FlightRoute> routes = routeRepository.findAllActiveRoutesAfter(now);

        Map<UUID, List<GraphEdge>> graph = new HashMap<>();

        for (FlightRoute route : routes) {
            UUID originCityId = route.getOriginCity().getId();

            GraphEdge edge = GraphEdge.builder()
                    .flightRouteId(route.getId())
                    .flightId(route.getFlight().getId())
                    .originCityId(originCityId)
                    .destCityId(route.getDestCity().getId())
                    .originCityCode(route.getOriginCity().getIataCityCode())
                    .destCityCode(route.getDestCity().getIataCityCode())
                    .weightPrice(route.getWeightPrice())
                    .weightDurationMin(route.getWeightDurationMin())
                    .minConnectionMin(route.getMinConnectionMin())
                    .departureAt(route.getDepartureAt())
                    .arrivalAt(route.getArrivalAt())
                    .airlineCode(route.getAirlineCode())
                    .airlineName(resolveAirlineName(route.getAirlineCode()))
                    .cabin(route.getCabin())
                    .currentPrice(route.getWeightPrice())
                    .build();

            graph.computeIfAbsent(originCityId, k -> new ArrayList<>()).add(edge);
        }

        graph.values().forEach(edges ->
                edges.sort(Comparator.comparingDouble(e ->
                        e.getWeightPrice().doubleValue()))
        );

        cacheService.storeGraph(graph);

        log.info("Flight graph built: {} cities, {} edges",
                graph.size(), routes.size());
        return graph;
    }

    @Scheduled(fixedDelayString = "${graph.cache.ttl-minutes:5}000")
    public void scheduledGraphRefresh() {
        log.debug("Scheduled graph refresh triggered");
        var uuidListMap = buildAndCacheGraph();
    }

    public void invalidateFlightEdges(UUID flightId) {
        log.info("Invalidating graph edges for flight: {}", flightId);
        Map<UUID, List<GraphEdge>> graph = cacheService.loadGraph();
        if (graph != null) {
            graph.values().forEach(edges ->
                    edges.removeIf(e -> e.getFlightId().equals(flightId))
            );
            cacheService.storeGraph(graph);
        }
    }

    private String resolveAirlineName(String code) {
        return switch (code) {
            case "TK" -> "Turkish Airlines";
            case "LH" -> "Lufthansa";
            case "AA" -> "American Airlines";
            case "BA" -> "British Airways";
            case "AF" -> "Air France";
            case "EK" -> "Emirates";
            case "SQ" -> "Singapore Airlines";
            case "JL" -> "Japan Airlines";
            case "AC" -> "Air Canada";
            case "KL" -> "KLM";
            case "QF" -> "Qantas";
            default   -> code;
        };
    }
}
