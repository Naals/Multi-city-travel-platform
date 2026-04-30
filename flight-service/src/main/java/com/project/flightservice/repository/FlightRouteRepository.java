package com.project.flightservice.repository;

import com.project.flightservice.model.FlightRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FlightRouteRepository extends JpaRepository<FlightRoute, UUID> {

    @Query("""
        SELECT r FROM FlightRoute r
        JOIN FETCH r.originCity oc
        JOIN FETCH r.destCity dc
        WHERE r.active = true
          AND r.departureAt > :now
        ORDER BY r.weightPrice ASC
    """)
    List<FlightRoute> findAllActiveRoutesAfter(@Param("now") java.time.Instant now);


    @Query("""
        SELECT r FROM FlightRoute r
        JOIN FETCH r.destCity dc
        WHERE r.originCity.id = :cityId
          AND r.active = true
          AND r.departureAt > :now
    """)
    List<FlightRoute> findActiveEdgesFromCity(
            @Param("cityId") UUID cityId,
            @Param("now")    java.time.Instant now
    );

    @Modifying
    @Query("UPDATE FlightRoute r SET r.active = false WHERE r.flight.id = :flightId")
    void deactivateByFlightId(@Param("flightId") UUID flightId);

    @Modifying
    @Query("""
        UPDATE FlightRoute r SET
            r.weightPrice = :price,
            r.weightDurationMin = :duration
        WHERE r.flight.id = :flightId
    """)
    void updateWeightsByFlightId(
            @Param("flightId") UUID flightId,
            @Param("price")    java.math.BigDecimal price,
            @Param("duration") int duration
    );
}
