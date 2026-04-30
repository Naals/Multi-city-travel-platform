package com.project.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
public class SavedRouteRequest {

    @NotNull
    private UUID userId;

    @NotBlank
    private String originCityCode;

    @NotBlank
    private String destCityCode;

    private String label;
}