package com.project.bookingservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BookingRequest {

    @NotNull(message = "Flight ID is required")
    private UUID flightId;

    private UUID tripId;

    @NotEmpty(message = "At least one passenger required")
    @Size(max = 9, message = "Maximum 9 passengers per booking")
    private List<@Valid PassengerRequest> passengers;

    @NotBlank(message = "Cabin class is required")
    @Pattern(regexp = "ECONOMY|PREMIUM_ECONOMY|BUSINESS|FIRST",
            message = "Invalid cabin class")
    private String cabinClass;

    @Data
    public static class PassengerRequest {
        @NotBlank private String firstName;
        @NotBlank private String lastName;
        @NotBlank private String passportNumber;
        @NotNull  private java.time.LocalDate dateOfBirth;
        @Pattern(regexp = "ADULT|CHILD|INFANT")
        private String type = "ADULT";
    }
}
