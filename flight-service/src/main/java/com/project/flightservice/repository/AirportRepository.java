package com.project.flightservice.repository;

import com.project.flightservice.model.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AirportRepository extends JpaRepository<Airport, UUID> {

    Optional<Airport> findByIataCodeIgnoreCase(String iataCode);

    @Query("SELECT a FROM Airport a JOIN FETCH a.city c WHERE c.iataCityCode = :cityCode AND a.active = true")
    List<Airport> findByCityIataCode(String cityCode);
}
