package com.project.reviewservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class CreateReviewRequest {

    @NotNull
    private UUID bookingId;

    @NotNull
    @Min(1) @Max(5)
    private Short overallRating;

    @Min(1) @Max(5)
    private Short punctualityRating;

    @Min(1) @Max(5)
    private Short comfortRating;

    @Min(1) @Max(5)
    private Short serviceRating;

    @Min(1) @Max(5)
    private Short valueRating;

    @Size(max = 200)
    private String title;

    @Size(max = 5000)
    private String comment;
}