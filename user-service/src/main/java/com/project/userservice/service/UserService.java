package com.project.userservice.service;

import com.project.userservice.dto.*;
import com.project.userservice.exception.*;
import com.project.userservice.mapper.UserMapper;
import com.project.userservice.model.SavedRoute;
import com.project.userservice.model.User;
import com.project.userservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository      userRepo;
    private final SavedRouteRepository savedRouteRepo;
    private final UserMapper          mapper;


    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepo.existsByEmailIgnoreCase(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "User with email already exists: " + request.getEmail());
        }
        // id is explicitly set to match auth-service credential UUID
        if (userRepo.existsById(request.getUserId())) {
            throw new UserAlreadyExistsException(
                    "User with ID already exists: " + request.getUserId());
        }
        User user = mapper.toEntity(request);
        user = userRepo.save(user);
        log.info("User profile created: {}", user.getId());
        return mapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID id) {
        return mapper.toDto(findUserOrThrow(id));
    }

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found with email: " + email));
        return mapper.toDto(user);
    }

    /**
     * PATCH update — only supplied fields are changed.
     * MapStruct's IGNORE null strategy handles this automatically.
     */
    @Transactional
    public UserDto updateUser(UUID id, UpdateUserRequest request) {
        User user = findUserOrThrow(id);
        mapper.updateEntityFromRequest(request, user);
        user = userRepo.save(user);
        log.info("User profile updated: {}", id);
        return mapper.toDto(user);
    }

    @Transactional
    public void deactivateUser(UUID id) {
        User user = findUserOrThrow(id);
        user.setActive(false);
        userRepo.save(user);
        log.info("User deactivated: {}", id);
    }


    @Transactional
    public SavedRouteDto saveRoute(UUID userId, SavedRouteRequest request) {
        User user = findUserOrThrow(userId);

        // Prevent duplicates
        savedRouteRepo.findByUserIdAndOriginCityCodeAndDestCityCode(
                        userId, request.getOriginCityCode(), request.getDestCityCode())
                .ifPresent(r -> {
                    throw new DuplicateSavedRouteException("Route already saved");
                });

        SavedRoute route = SavedRoute.builder()
                .user(user)
                .originCityCode(request.getOriginCityCode().toUpperCase())
                .destCityCode(request.getDestCityCode().toUpperCase())
                .label(request.getLabel())
                .build();

        route = savedRouteRepo.save(route);
        return toSavedRouteDto(route);
    }

    @Transactional(readOnly = true)
    public List<SavedRouteDto> getSavedRoutes(UUID userId) {
        findUserOrThrow(userId);   // validate user exists
        return savedRouteRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toSavedRouteDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSavedRoute(UUID userId, UUID routeId) {
        findUserOrThrow(userId);
        savedRouteRepo.deleteByUserIdAndId(userId, routeId);
    }

    // ── gRPC support methods

    @Transactional(readOnly = true)
    public User findUserOrThrow(UUID id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found: " + id));
    }

    @Transactional(readOnly = true)
    public boolean isUserActiveById(UUID id) {
        return userRepo.existsByIdAndActive(id);
    }


    private SavedRouteDto toSavedRouteDto(SavedRoute route) {
        return SavedRouteDto.builder()
                .id(route.getId())
                .userId(route.getUser().getId())
                .originCityCode(route.getOriginCityCode())
                .destCityCode(route.getDestCityCode())
                .label(route.getLabel())
                .createdAt(route.getCreatedAt())
                .build();
    }
}
