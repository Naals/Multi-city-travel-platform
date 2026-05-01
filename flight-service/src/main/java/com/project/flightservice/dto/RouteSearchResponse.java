package com.project.flightservice.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class RouteSearchResponse {
    private String           originCityCode;
    private String           destCityCode;
    private int              totalRoutesFound;
    private List<RouteOption> routes;

    @Data
    @Builder
    public static class RouteOption {
        private int              rank;
        private double           totalPrice;
        private int              totalDurationMinutes;
        private int              stopCount;
        private Instant          departureAt;
        private Instant          arrivalAt;
        private List<FlightLeg>  legs;
    }

    @Data
    @Builder
    public static class FlightLeg {
        private int      legNumber;
        private String   flightNumber;
        private String   airlineCode;
        private String   airlineName;
        private String   originCityCode;
        private String   destCityCode;
        private Instant  departureAt;
        private Instant  arrivalAt;
        private int      durationMinutes;
        private double   price;
        private String   cabin;
        private Integer  layoverMinutesAfter;  // null for last leg
    }
}
