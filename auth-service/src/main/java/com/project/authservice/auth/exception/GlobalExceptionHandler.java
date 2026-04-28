package com.project.authservice.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            fieldErrors.put(field, error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .status(400)
                        .error("Validation Failed")
                        .message("One or more fields are invalid")
                        .fieldErrors(fieldErrors)
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailExists(EmailAlreadyExistsException ex) {
        return conflict(ex.getMessage());
    }

    @ExceptionHandler({BadCredentialsException.class, InvalidTokenException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.of(401, "Unauthorized", ex.getMessage())
        );
    }

    @ExceptionHandler({AccountLockedException.class, AccountDisabledException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.of(403, "Forbidden", ex.getMessage())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError().body(
                ErrorResponse.of(500, "Internal Server Error",
                        "An unexpected error occurred")
        );
    }

    private ResponseEntity<ErrorResponse> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.of(409, "Conflict", message)
        );
    }
}
