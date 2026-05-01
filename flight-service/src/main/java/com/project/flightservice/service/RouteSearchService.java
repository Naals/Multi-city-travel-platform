package com.project.flightservice.service;

import com.project.flightservice.algorithm.*;
import com.project.flightservice.cache.GraphCacheService;
import com.project.flightservice.dto.*;
import com.project.flightservice.exception.*;
import com.project.flightservice.model.City;
import com.project.flightservice.repository.CityRepository;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteSearchService {

    private final DijkstraEngine       dijkstra;
    private final GraphCacheService    cacheService;
    private final GraphBuilderService  graphBuilder;
    private final CityRepository       cityRepository;

    @Value("${graph.dijkstra.min-layover-minutes:60}")
    private int minLayoverMinutes;

    @Observed(name = "route.search", contextualName = "dijkstra-route-search")
    public RouteSearchResponse search(RouteSearchRequest request) {

        log.info("Route search: {} → {} on {} sortBy={} maxStops={}",
                request.getOriginCityCode(), request.getDestCityCode(),
                request.getDepartureDate(), request.getSortBy(), request.getMaxStops());

        City origin = resolveCityOrThrow(request.getOriginCityCode());
        City dest   = resolveCityOrThrow(request.getDestCityCode());

        if (origin.getId().equals(dest.getId())) {
            throw new InvalidRouteException("Origin and destination cannot be the same city");
        }

        Map<UUID, List<GraphEdge>> graph = loadOrBuildGraph();

        if (request.getPreferredAirline() != null
                || request.getCabinClass() != null) {
            graph = applyPreFilters(graph, request);
        }

        Instant searchFrom = request.getDepartureDate()
                .atStartOfDay(ZoneOffset.UTC).toInstant();

        List<RouteResult> rawResults = dijkstra.search(
                graph,
                origin.getId(),
                dest.getId(),
                request.getSortBy(),
                request.getMaxStops(),
                request.getTopK(),
                searchFrom
        );

        if (rawResults.isEmpty()) {
            log.info("No routes found: {} → {}", origin.getIataCityCode(), dest.getIataCityCode());
            throw new NoRouteFoundException(
                    String.format("No routes found from %s to %s on %s. " +
                                    "Try increasing max stops or changing the date.",
                            request.getOriginCityCode(),
                            request.getDestCityCode(),
                            request.getDepartureDate())
            );
        }

        return buildResponse(request, rawResults);
    }


    private Map<UUID, List<GraphEdge>> loadOrBuildGraph() {
        Map<UUID, List<GraphEdge>> graph = cacheService.loadGraph();
        if (graph == null || graph.isEmpty()) {
            log.info("Graph cache miss — triggering rebuild");
            graph = graphBuilder.buildAndCacheGraph();
        }
        return graph;
    }

    private Map<UUID, List<GraphEdge>> applyPreFilters(
            Map<UUID, List<GraphEdge>> original,
            RouteSearchRequest request) {

        Map<UUID, List<GraphEdge>> filtered = new HashMap<>();
        original.forEach((cityId, edges) -> {
            List<GraphEdge> kept = edges.stream()
                    .filter(e -> {
                        boolean airlineOk = request.getPreferredAirline() == null
                                || e.getAirlineCode().equalsIgnoreCase(request.getPreferredAirline());
                        boolean cabinOk = request.getCabinClass() == null
                                || e.getCabin().name().equalsIgnoreCase(request.getCabinClass());
                        return airlineOk && cabinOk;
                    })
                    .toList();
            if (!kept.isEmpty()) {
                filtered.put(cityId, kept);
            }
        });
        return filtered;
    }

    private City resolveCityOrThrow(String cityCode) {
        return cityRepository.findByIataCityCodeIgnoreCase(cityCode)
                .orElseThrow(() -> new CityNotFoundException(
                        "City not found for code: " + cityCode.toUpperCase()));
    }

    private RouteSearchResponse buildResponse(
            RouteSearchRequest request,
            List<RouteResult> results) {

        List<RouteSearchResponse.RouteOption> options = IntStream
                .range(0, results.size())
                .mapToObj(i -> buildRouteOption(results.get(i), i + 1))
                .toList();

        return RouteSearchResponse.builder()
                .originCityCode(request.getOriginCityCode().toUpperCase())
                .destCityCode(request.getDestCityCode().toUpperCase())
                .totalRoutesFound(results.size())
                .routes(options)
                .build();
    }

    private RouteSearchResponse.RouteOption buildRouteOption(RouteResult result, int rank) {
        List<GraphEdge> edges = result.getEdges();
        List<RouteSearchResponse.FlightLeg> legs = new ArrayList<>();

        for (int i = 0; i < edges.size(); i++) {
            GraphEdge edge = edges.get(i);

            Integer layoverMinutes = null;
            if (i < edges.size() - 1) {
                GraphEdge next = edges.get(i + 1);
                layoverMinutes = (int) Duration
                        .between(edge.getArrivalAt(), next.getDepartureAt())
                        .toMinutes();
            }

            legs.add(RouteSearchResponse.FlightLeg.builder()
                    .legNumber(i + 1)
                    .airlineCode(edge.getAirlineCode())
                    .airlineName(edge.getAirlineName())
                    .originCityCode(edge.getOriginCityCode())
                    .destCityCode(edge.getDestCityCode())
                    .departureAt(edge.getDepartureAt())
                    .arrivalAt(edge.getArrivalAt())
                    .durationMinutes(edge.getWeightDurationMin())
                    .price(edge.getWeightPrice().doubleValue())
                    .cabin(edge.getCabin().name())
                    .layoverMinutesAfter(layoverMinutes)
                    .build());
        }

        return RouteSearchResponse.RouteOption.builder()
                .rank(rank)
                .totalPrice(result.getTotalPrice())
                .totalDurationMinutes(result.getTotalDurationMinutes())
                .stopCount(result.getStopCount())
                .departureAt(result.getDepartureAt())
                .arrivalAt(result.getArrivalAt())
                .legs(legs)
                .build();
    }
}
