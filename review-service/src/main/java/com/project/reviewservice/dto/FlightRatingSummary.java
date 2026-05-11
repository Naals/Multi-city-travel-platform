package com.project.reviewservice.dto;

import lombok.*;
import java.util.UUID;

@Data @Builder
public class FlightRatingSummary {
    private UUID   flightId;
    private long   reviewCount;
    private double avgOverall;
    private double avgPunctuality;
    private double avgComfort;
    private double avgService;
    private double avgValue;
}