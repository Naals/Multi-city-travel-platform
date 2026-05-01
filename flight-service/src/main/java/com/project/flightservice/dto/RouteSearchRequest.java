package com.project.flightservice.dto;

import com.project.flightservice.algorithm.SortStrategy;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class RouteSearchRequest {

    @NotBlank(message = "Origin city code is required")
    @Size(min = 2, max = 3, message = "City code must be 2-3 characters")
    private String originCityCode;

    @NotBlank(message = "Destination city code is required")
    @Size(min = 2, max = 3, message = "City code must be 2-3 characters")
    private String destCityCode;

    @NotNull(message = "Departure date is required")
    @Future(message = "Departure date must be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureDate;

    @Min(value = 0, message = "Max stops cannot be negative")
    @Max(value = 5, message = "Max stops cannot exceed 5")
    private int maxStops = 2;

    private SortStrategy sortBy = SortStrategy.PRICE;

    @Min(1) @Max(5)
    private int topK = 3;

    private String preferredAirline;
    private String cabinClass;
}
