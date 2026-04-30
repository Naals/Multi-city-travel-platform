package com.project.flightservice.repository;

import com.project.flightservice.model.Flight;
import com.project.flightservice.model.FlightStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlightRepository extends JpaRepository<Flight, UUID> {

    Optional<Flight> findByFlightNumberAndScheduledDeparture(
            String flightNumber, Instant departure
    );

    @Query("""
        SELECT f FROM Flight f
        JOIN FETCH f.originAirport oa
        JOIN FETCH f.destAirport da
        WHERE oa.iataCode = :origin
          AND da.iataCode = :dest
          AND f.scheduledDeparture BETWEEN :from AND :to
          AND f.status = 'SCHEDULED'
          AND f.active = true
          AND (f.seatsTotal - f.seatsBooked) > 0
        ORDER BY f.currentPrice ASC
    """)
    List<Flight> findBookableFlights(
            @Param("origin") String origin,
            @Param("dest")   String dest,
            @Param("from")   Instant from,
            @Param("to")     Instant to
    );

    @Query("SELECT f FROM Flight f WHERE f.id = :id")
    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<Flight> findByIdWithLock(@Param("id") UUID id);

    @Modifying
    @Query("""
        UPDATE Flight f SET
            f.status = :status,
            f.delayMinutes = :delayMinutes,
            f.delayReason = :reason,
            f.actualDeparture = :actualDeparture
        WHERE f.id = :id
    """)
    int updateFlightStatus(
            @Param("id")             UUID id,
            @Param("status")         FlightStatus status,
            @Param("delayMinutes")   int delayMinutes,
            @Param("reason")         String reason,
            @Param("actualDeparture") Instant actualDeparture
    );
}
