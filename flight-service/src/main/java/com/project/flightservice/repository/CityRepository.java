package com.project.flightservice.repository;

import com.project.flightservice.model.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CityRepository extends JpaRepository<City, UUID> {

    Optional<City> findByIataCityCodeIgnoreCase(String iataCode);

    List<City> findByActiveTrue();


    @Query("SELECT c.id FROM City c WHERE c.active = true")
    List<UUID> findAllActiveCityIds();
}
