package com.project.reviewservice.service;

import com.project.reviewservice.dto.*;
import com.project.reviewservice.exception.*;
import com.project.reviewservice.model.*;
import com.project.reviewservice.repository.*;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository            reviewRepo;
    private final ReviewEligibilityRepository eligibilityRepo;
    private final MaterializedViewRefreshService mvRefreshService;


    @Observed(name = "review.submit")
    @Transactional
    public ReviewDto submitReview(UUID userId, CreateReviewRequest request) {

        ReviewEligibility eligibility = eligibilityRepo
                .findByUserIdAndBookingId(userId, request.getBookingId())
                .orElseThrow(() -> new ReviewNotEligibleException(
                        "You are not eligible to review this booking. " +
                                "The booking must be completed before a review can be submitted."
                ));

        if (eligibility.isReviewed()) {
            throw new ReviewAlreadySubmittedException(
                    "You have already submitted a review for booking: "
                            + request.getBookingId()
            );
        }

        if (reviewRepo.existsByBookingId(request.getBookingId())) {
            throw new ReviewAlreadySubmittedException(
                    "A review already exists for this booking"
            );
        }

        Review review = Review.builder()
                .eligibility(eligibility)
                .userId(userId)
                .flightId(eligibility.getFlightId())
                .bookingId(request.getBookingId())
                .overallRating(request.getOverallRating())
                .punctualityRating(request.getPunctualityRating())
                .comfortRating(request.getComfortRating())
                .serviceRating(request.getServiceRating())
                .valueRating(request.getValueRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .published(true)
                .flagged(false)
                .helpfulVotes(0)
                .build();

        review = reviewRepo.save(review);

        eligibilityRepo.markReviewed(eligibility.getId());

        mvRefreshService.scheduleRefresh(eligibility.getFlightId());

        log.info("Review submitted: reviewId={} userId={} flightId={} rating={}",
                review.getId(), userId,
                eligibility.getFlightId(), request.getOverallRating());

        return toDto(review);
    }


    @Transactional(readOnly = true)
    public List<ReviewDto> getFlightReviews(UUID flightId) {
        return reviewRepo
                .findByFlightIdAndPublishedTrueOrderByCreatedAtDesc(flightId)
                .stream().map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getMyReviews(UUID userId) {
        return reviewRepo
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FlightRatingSummary getFlightRatingSummary(UUID flightId) {
        Double avg   = reviewRepo.findAvgRatingByFlightId(flightId);
        Long   count = reviewRepo.countByFlightId(flightId);

        return FlightRatingSummary.builder()
                .flightId(flightId)
                .reviewCount(count != null ? count : 0L)
                .avgOverall(avg != null ? Math.round(avg * 100.0) / 100.0 : 0.0)
                .build();
    }


    @Transactional
    public void flagReview(UUID reviewId, String reason) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(
                        "Review not found: " + reviewId));
        review.setFlagged(true);
        review.setFlagReason(reason);
        review.setPublished(false);
        reviewRepo.save(review);
        log.info("Review flagged: reviewId={} reason={}", reviewId, reason);
    }

    @Transactional
    public void voteHelpful(UUID reviewId) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(
                        "Review not found: " + reviewId));
        review.setHelpfulVotes(review.getHelpfulVotes() + 1);
        reviewRepo.save(review);
    }


    private ReviewDto toDto(Review r) {
        return ReviewDto.builder()
                .id(r.getId())
                .userId(r.getUserId())
                .flightId(r.getFlightId())
                .bookingId(r.getBookingId())
                .overallRating(r.getOverallRating())
                .punctualityRating(r.getPunctualityRating())
                .comfortRating(r.getComfortRating())
                .serviceRating(r.getServiceRating())
                .valueRating(r.getValueRating())
                .title(r.getTitle())
                .comment(r.getComment())
                .helpfulVotes(r.getHelpfulVotes())
                .createdAt(r.getCreatedAt())
                .build();
    }
}