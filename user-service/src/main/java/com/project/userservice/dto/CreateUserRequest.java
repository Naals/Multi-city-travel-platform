package com.project.userservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
public class CreateUserRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank @Size(max = 100)
    private String firstName;

    @NotBlank @Size(max = 100)
    private String lastName;

    @NotBlank @Email @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String phone;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private String gender;
    private String nationality;

    @Size(max = 50)
    private String passportNumber;

    @Future(message = "Passport must not be expired")
    private LocalDate passportExpiry;

    private Map<String, Object> preferences;
}
