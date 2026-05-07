package com.project.paymentservice.exception;

import lombok.Builder;
import lombok.Data;
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
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e ->
                errors.put(((FieldError) e).getField(), e.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .status(400).error("Validation Failed")
                .message("Invalid request").fieldErrors(errors)
                .timestamp(Instant.now()).build());
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException ex) {
        return ResponseEntity.status(404).body(
                ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler({RefundNotAllowedException.class, DuplicatePaymentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(
                ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        log.error("Unhandled exception in payment-service", ex);
        return ResponseEntity.internalServerError().body(
                ErrorResponse.of(500, "Internal Server Error",
                        "An unexpected error occurred"));
    }

    @Data @Builder
    public static class ErrorResponse {
        private int                 status;
        private String              error;
        private String              message;
        private Map<String, String> fieldErrors;
        private Instant             timestamp;

        public static ErrorResponse of(int status, String error, String message) {
            return ErrorResponse.builder()
                    .status(status).error(error).message(message)
                    .timestamp(Instant.now()).build();
        }
    }
}