package com.project.bookingservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e ->
                errors.put(((FieldError) e).getField(), e.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .status(400).error("Validation Failed")
                .message("Invalid booking request")
                .fieldErrors(errors).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(BookingNotFoundException ex) {
        return ResponseEntity.status(404).body(
                ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(BookingAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(BookingAccessDeniedException ex) {
        return ResponseEntity.status(403).body(
                ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler({BookingNotAllowedException.class,
            FlightNotBookableException.class,
            InsufficientSeatsException.class,
            BookingNotCancellableException.class,
            BookingAlreadyCancelledException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(
                ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler({FlightServiceUnavailableException.class,
            UserValidationException.class})
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(RuntimeException ex) {
        return ResponseEntity.status(503).body(
                ErrorResponse.of(503, "Service Unavailable", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        log.error("Unhandled exception in booking-service", ex);
        return ResponseEntity.internalServerError().body(
                ErrorResponse.of(500, "Internal Server Error", "An unexpected error occurred"));
    }
}
