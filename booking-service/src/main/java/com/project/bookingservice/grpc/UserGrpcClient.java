package com.project.bookingservice.grpc;


import com.project.proto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class UserGrpcClient {


    @GrpcClient("user-service")
    private UserGrpcServiceGrpc.UserGrpcServiceBlockingStub userStub;


    @CircuitBreaker(name = "userGrpc", fallbackMethod = "validateUserFallback")
    @Retry(name = "userGrpc")
    public ValidateUserResponse validateUser(UUID userId) {
        log.debug("gRPC ValidateUser: {}", userId);
        ValidateUserRequest request = ValidateUserRequest.newBuilder()
                .setUserId(userId.toString())
                .build();
        return userStub
                .withDeadlineAfter(3, TimeUnit.SECONDS)
                .validateUser(request);
    }

    public ValidateUserResponse validateUserFallback(UUID userId, Throwable ex) {
        log.error("ValidateUser fallback triggered for userId={}: {}",
                userId, ex.getMessage());
        throw new UserValidationException(
                "User validation service is currently unavailable. " +
                        "Please try again in a few seconds."
        );
    }


    @CircuitBreaker(name = "userGrpc", fallbackMethod = "getUserProfileFallback")
    @Retry(name = "userGrpc")
    public UserProfileResponse getUserProfile(UUID userId) {
        log.debug("gRPC GetUserProfile: {}", userId);
        GetUserByIdRequest request = GetUserByIdRequest.newBuilder()
                .setUserId(userId.toString())
                .build();
        return userStub.getUserProfile(request);
    }

    public UserProfileResponse getUserProfileFallback(UUID userId, Throwable ex) {
        log.warn("GetUserProfile fallback triggered for userId={}: {}",
                userId, ex.getMessage());
        return UserProfileResponse.newBuilder()
                .setUserId(userId.toString())
                .setFirstName("Traveler")
                .setLastName("")
                .setEmail("unknown@travel.com")
                .build();
    }
}
