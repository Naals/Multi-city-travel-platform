package com.project.userservice.grpc;

import com.project.proto.*;
import com.project.userservice.model.*;
import com.project.userservice.service.UserService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcServer extends UserGrpcServiceGrpc.UserGrpcServiceImplBase {

    private final UserService userService;



    @Override
    public void getUserById(
            GetUserByIdRequest request,
            StreamObserver<UserResponse> responseObserver) {
        try {
            UUID userId = parseUuid(request.getUserId(), responseObserver);
            if (userId == null) return;

            User user = userService.findUserOrThrow(userId);

            UserResponse response = UserResponse.newBuilder()
                    .setUserId(user.getId().toString())
                    .setFirstName(user.getFirstName())
                    .setLastName(user.getLastName())
                    .setEmail(user.getEmail())
                    .setPhone(nullSafe(user.getPhone()))
                    .setNationality(nullSafe(user.getNationality()))
                    .setPassportNumber(nullSafe(user.getPassportNumber()))
                    .setPassportExpiry(user.getPassportExpiry() != null
                            ? user.getPassportExpiry().toString() : "")
                    .setIsActive(user.isActive())
                    .setCreatedAt(user.getCreatedAt().toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.debug("gRPC GetUserById success: {}", userId);

        } catch (UserNotFoundException ex) {
            log.warn("gRPC GetUserById - user not found: {}", request.getUserId());
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(ex.getMessage())
                            .asRuntimeException()
            );
        } catch (Exception ex) {
            log.error("gRPC GetUserById unexpected error", ex);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asRuntimeException()
            );
        }
    }



    @Override
    public void getUserProfile(
            GetUserByIdRequest request,
            StreamObserver<UserProfileResponse> responseObserver) {
        try {
            UUID userId = parseUuid(request.getUserId(), responseObserver);
            if (userId == null) return;

            User user = userService.findUserOrThrow(userId);

            UserProfileResponse response = UserProfileResponse.newBuilder()
                    .setUserId(user.getId().toString())
                    .setFirstName(user.getFirstName())
                    .setLastName(user.getLastName())
                    .setEmail(user.getEmail())
                    .setPhone(nullSafe(user.getPhone()))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (UserNotFoundException ex) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException()
            );
        } catch (Exception ex) {
            log.error("gRPC GetUserProfile unexpected error", ex);
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal server error").asRuntimeException()
            );
        }
    }

    @Override
    public void validateUser(
            ValidateUserRequest request,
            StreamObserver<ValidateUserResponse> responseObserver) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            boolean exists = userService.isUserActiveById(userId);

            ValidateUserResponse.Builder builder = ValidateUserResponse.newBuilder()
                    .setIsActive(exists)
                    .setIsValid(exists);

            if (!exists) {
                try {
                    userService.findUserOrThrow(userId);
                    builder.setReason("ACCOUNT_INACTIVE");
                } catch (UserNotFoundException e) {
                    builder.setReason("NOT_FOUND");
                }
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

        } catch (IllegalArgumentException ex) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid UUID format: " + request.getUserId())
                            .asRuntimeException()
            );
        } catch (Exception ex) {
            log.error("gRPC ValidateUser unexpected error", ex);
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal server error").asRuntimeException()
            );
        }
    }


    private <T> UUID parseUuid(String raw, StreamObserver<T> observer) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            observer.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid UUID: " + raw)
                            .asRuntimeException()
            );
            return null;
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
