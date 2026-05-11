package com.project.reviewservice.dto;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class ReviewDto {
    private UUID    id;
    private UUID    userId;
    private UUID    flightId;
    private UUID    bookingId;
    private Short   overallRating;
    private Short   punctualityRating;
    private Short   comfortRating;
    private Short   serviceRating;
    private Short   valueRating;
    private String  title;
    private String  comment;
    private int     helpfulVotes;
    private Instant createdAt;
}