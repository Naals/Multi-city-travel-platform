package com.project.userservice.repository;

import com.project.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    // Lightweight existence + active check for gRPC ValidateUser
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.id = :id AND u.active = true")
    boolean existsByIdAndActive(@Param("id") UUID id);

    // Fetch only fields needed for gRPC UserProfileResponse 
    @Query("SELECT u.id, u.firstName, u.lastName, u.email, u.phone FROM User u WHERE u.id = :id")
    Optional<Object[]> findProfileById(@Param("id") UUID id);
}
