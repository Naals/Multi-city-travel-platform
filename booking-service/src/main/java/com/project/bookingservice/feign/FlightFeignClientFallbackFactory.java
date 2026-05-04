package com.project.bookingservice.feign;


import com.project.bookingservice.exception.FlightServiceUnavailableException;
import com.project.bookingservice.feign.dto.FlightAvailabilityResponse;
import com.project.bookingservice.feign.dto.SeatLockRequest;
import com.project.bookingservice.feign.dto.SeatLockResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class FlightFeignClientFallbackFactory
        implements FallbackFactory<FlightFeignClient> {

    @Override
    public FlightFeignClient create(Throwable cause) {
        return new FlightFeignClient() {

            @Override
            public FlightAvailabilityResponse checkAvailability(UUID flightId) {
                log.error("Flight availability check failed for flightId={}: {}",
                        flightId, cause.getMessage());
                throw new FlightServiceUnavailableException(
                        "Flight availability check is currently unavailable. " +
                                "Please try again shortly."
                );
            }

            @Override
            public SeatLockResponse lockSeat(UUID flightId, SeatLockRequest request) {
                log.error("Seat lock failed for flightId={}: {}",
                        flightId, cause.getMessage());
                throw new FlightServiceUnavailableException(
                        "Seat reservation is currently unavailable. Please try again."
                );
            }

            @Override
            public void releaseSeat(UUID flightId, SeatLockRequest request) {
                log.error("Seat release failed for flightId={} — " +
                                "will be cleaned up by reconciliation job: {}",
                        flightId, cause.getMessage());
            }
        };
    }
}
