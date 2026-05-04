package com.project.bookingservice.service;

import com.project.bookingservice.dto.*;
import com.project.bookingservice.exception.*;
import com.project.bookingservice.feign.FlightFeignClient;
import com.project.bookingservice.feign.dto.*;
import com.project.bookingservice.grpc.UserGrpcClient;
import com.project.bookingservice.model.*;
import com.project.bookingservice.rabbitmq.PaymentEventPublisher;
import com.project.bookingservice.rabbitmq.event.PaymentRequestEvent;
import com.project.bookingservice.repository.*;
import com.project.bookingservice.repository.repository.BookingEventRepository;
import com.project.proto.ValidateUserResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository      bookingRepo;
    private final BookingEventRepository eventRepo;
    private final UserGrpcClient         userGrpcClient;
    private final FlightFeignClient      flightFeignClient;
    private final PaymentEventPublisher  paymentPublisher;


    @Observed(name = "booking.create")
    @Bulkhead(name = "bookingService")
    @Transactional
    public BookingResponse createBooking(UUID userId, BookingRequest request) {

        // ── Step 1: Validate user via gRPC ───────────────────
        ValidateUserResponse userValidation =
                userGrpcClient.validateUser(userId);

        if (!userValidation.getIsValid()) {
            throw new BookingNotAllowedException(
                    "User validation failed: " + userValidation.getReason()
            );
        }

        FlightAvailabilityResponse flight =
                flightFeignClient.checkAvailability(request.getFlightId());

        if (!flight.isBookable()) {
            throw new FlightNotBookableException(
                    String.format("Flight %s is not available for booking. Status: %s",
                            flight.getFlightNumber(), flight.getStatus())
            );
        }

        if (flight.getSeatsAvailable() < request.getPassengers().size()) {
            throw new InsufficientSeatsException(
                    String.format("Only %d seat(s) available, %d requested",
                            flight.getSeatsAvailable(),
                            request.getPassengers().size())
            );
        }

        UUID bookingId = UUID.randomUUID();
        SeatLockResponse seatLock = flightFeignClient.lockSeat(
                request.getFlightId(),
                new SeatLockRequest(bookingId, request.getCabinClass(),
                        request.getPassengers().size())
        );

        UUID tripId = request.getTripId() != null
                ? request.getTripId()
                : UUID.randomUUID();   // new trip for first leg

        List<Map<String, Object>> passengerMaps = buildPassengerMaps(request);

        Booking booking = Booking.builder()
                .id(bookingId)
                .tripId(tripId)
                .userId(userId)
                .flightId(request.getFlightId())
                .flightNumber(flight.getFlightNumber())
                .originIata(flight.getOriginIata())
                .destIata(flight.getDestIata())
                .departureAt(flight.getDepartureAt())
                .arrivalAt(flight.getArrivalAt())
                .cabin(request.getCabinClass())
                .pricePaid(flight.getCurrentPrice())
                .currency(flight.getCurrency())
                .seatNumber(seatLock.getSeatNumber())
                .passengers(passengerMaps)
                .status(BookingStatus.PENDING)
                .build();

        booking = bookingRepo.save(booking);

        auditEvent(booking.getId(), null, BookingStatus.PENDING, "USER",
                "Booking created for flight " + flight.getFlightNumber());

        paymentPublisher.publishPaymentRequest(PaymentRequestEvent.builder()
                .bookingId(booking.getId().toString())
                .userId(userId.toString())
                .amount(booking.getPricePaid())
                .currency(booking.getCurrency())
                .flightNumber(booking.getFlightNumber())
                .build());

        log.info("Booking created: {} for user={} flight={}",
                bookingId, userId, request.getFlightId());

        return toResponse(booking);
    }



    @Transactional
    public void confirmBooking(UUID bookingId) {
        Booking booking = findOrThrow(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            log.warn("Confirm called on non-PENDING booking {}: {}",
                    bookingId, booking.getStatus());
            return;
        }

        bookingRepo.confirmBooking(bookingId, BookingStatus.CONFIRMED);
        auditEvent(bookingId, BookingStatus.PENDING, BookingStatus.CONFIRMED,
                "PAYMENT_SERVICE", "Payment confirmed");

        log.info("Booking confirmed: {}", bookingId);
    }


    @Transactional
    public void cancelBooking(UUID bookingId, UUID userId,
                              CancellationReason reason, String note) {

        Booking booking = findOrThrow(bookingId);

        if (!booking.getUserId().equals(userId)) {
            throw new BookingAccessDeniedException(
                    "You do not have permission to cancel this booking"
            );
        }

        validateCancellationAllowed(booking);

        BookingStatus prevStatus = booking.getStatus();

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(Instant.now());
        booking.setCancelNote(note);
        bookingRepo.save(booking);

        try {
            flightFeignClient.releaseSeat(
                    booking.getFlightId(),
                    new SeatLockRequest(bookingId, booking.getCabin(), 1)
            );
        } catch (Exception ex) {
            log.error("Seat release failed for booking {} — reconciliation needed", bookingId);
        }

        auditEvent(bookingId, prevStatus, BookingStatus.CANCELLED,
                "USER", "Cancelled: " + note);

        if (prevStatus == BookingStatus.CONFIRMED) {
            paymentPublisher.publishRefundRequest(
                    booking.getId().toString(),
                    booking.getPricePaid()
            );
        }

        log.info("Booking cancelled: {} reason={}", bookingId, reason);
    }

    @Transactional
    public void completeBooking(UUID bookingId) {
        Booking booking = findOrThrow(bookingId);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            return;
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(Instant.now());
        bookingRepo.save(booking);

        auditEvent(bookingId, BookingStatus.CONFIRMED, BookingStatus.COMPLETED,
                "SYSTEM", "Flight arrived");

        log.info("Booking completed: {}", bookingId);
    }


    @Transactional
    public void cancelAllForFlight(UUID flightId, String reason) {
        List<Booking> affected = bookingRepo.findByFlightIdAndStatusIn(
                flightId,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        );

        int cancelled = bookingRepo.cancelAllForFlight(flightId);

        affected.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .forEach(b -> {
                    paymentPublisher.publishRefundRequest(
                            b.getId().toString(), b.getPricePaid()
                    );
                    auditEvent(b.getId(), b.getStatus(), BookingStatus.CANCELLED,
                            "FLIGHT_SERVICE", "Flight cancelled: " + reason);
                });

        log.info("Cancelled {} bookings for flight {}", cancelled, flightId);
    }


    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID bookingId, UUID userId) {
        Booking booking = findOrThrow(bookingId);
        if (!booking.getUserId().equals(userId)) {
            throw new BookingAccessDeniedException("Access denied");
        }
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(UUID userId) {
        return bookingRepo.findByUserIdOrderByBookedAtDesc(userId)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getTripBookings(UUID tripId) {
        return bookingRepo.findByTripIdOrderByDepartureAt(tripId)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }


    private void validateCancellationAllowed(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingAlreadyCancelledException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BookingNotCancellableException(
                    "Completed bookings cannot be cancelled");
        }
        if (booking.getDepartureAt().isBefore(
                Instant.now().plusSeconds(7200))) {
            throw new BookingNotCancellableException(
                    "Cancellation is not allowed within 2 hours of departure");
        }
    }

    private void auditEvent(UUID bookingId, BookingStatus from,
                            BookingStatus to, String triggeredBy, String note) {
        eventRepo.save(BookingEvent.builder()
                .bookingId(bookingId)
                .fromStatus(from)
                .toStatus(to)
                .triggeredBy(triggeredBy)
                .note(note)
                .occurredAt(Instant.now())
                .build());
    }

    private Booking findOrThrow(UUID id) {
        return bookingRepo.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found: " + id));
    }

    private List<Map<String, Object>> buildPassengerMaps(BookingRequest request) {
        return request.getPassengers().stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("firstName",      p.getFirstName());
            map.put("lastName",       p.getLastName());
            map.put("passportNumber", p.getPassportNumber());
            map.put("dateOfBirth",    p.getDateOfBirth().toString());
            map.put("type",           p.getType());
            return map;
        }).collect(Collectors.toList());
    }

    private BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .tripId(b.getTripId())
                .userId(b.getUserId())
                .flightId(b.getFlightId())
                .flightNumber(b.getFlightNumber())
                .originIata(b.getOriginIata())
                .destIata(b.getDestIata())
                .departureAt(b.getDepartureAt())
                .arrivalAt(b.getArrivalAt())
                .cabin(b.getCabin())
                .pricePaid(b.getPricePaid())
                .currency(b.getCurrency())
                .seatNumber(b.getSeatNumber())
                .status(b.getStatus().name())
                .passengers(b.getPassengers())
                .bookedAt(b.getBookedAt())
                .confirmedAt(b.getConfirmedAt())
                .build();
    }
}