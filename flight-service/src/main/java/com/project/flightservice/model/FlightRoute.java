package com.project.flightservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * FlightRoute — one directed edge in the Dijkstra graph.
 */
@Entity
@Table(name = "flight_routes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FlightRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_city_id", nullable = false)
    private City originCity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dest_city_id", nullable = false)
    private City destCity;

    @Column(name = "weight_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal weightPrice;

    @Column(name = "weight_duration_min", nullable = false)
    private int weightDurationMin;

    @Column(name = "min_connection_min", nullable = false)
    @Builder.Default
    private int minConnectionMin = 60;

    @Column(name = "departure_at", nullable = false)
    private Instant departureAt;

    @Column(name = "arrival_at", nullable = false)
    private Instant arrivalAt;

    @Column(name = "airline_code", nullable = false, length = 2)
    private String airlineCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CabinClass cabin;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
