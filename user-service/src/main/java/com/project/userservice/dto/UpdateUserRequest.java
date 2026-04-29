package com.project.userservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
public class UpdateUserRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 30)
    private String phone;

    @Past
    private LocalDate dateOfBirth;

    private String gender;
    private String nationality;

    @Size(max = 50)
    private String passportNumber;

    @Future
    private LocalDate passportExpiry;

    private Map<String, Object> preferences;

    @Size(max = 500)
    private String avatarUrl;
}
