package com.project.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationality;
    private String passportNumber;
    private LocalDate passportExpiry;
    private Map<String, Object> preferences;
    private String avatarUrl;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
