package com.project.userservice.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SavedRouteDto {

    private UUID id;
    private UUID userId;
    private String originCityCode;
    private String destCityCode;
    private String label;
    private Instant createdAt;
}