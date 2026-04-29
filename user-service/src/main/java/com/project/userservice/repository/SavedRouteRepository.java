package com.project.userservice.repository;

import com.project.userservice.model.SavedRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedRouteRepository extends JpaRepository<SavedRoute, UUID> {

    List<SavedRoute> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<SavedRoute> findByUserIdAndOriginCityCodeAndDestCityCode(
            UUID userId, String origin, String dest
    );

    void deleteByUserIdAndId(UUID userId, UUID routeId);
}
