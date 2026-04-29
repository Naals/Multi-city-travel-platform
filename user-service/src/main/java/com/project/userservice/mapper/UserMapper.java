package com.project.userservice.mapper;

import com.project.userservice.dto.*;
import com.project.userservice.model.*;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface UserMapper {

    @Mapping(source = "userId", target = "id")
    @Mapping(target = "active",    constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(CreateUserRequest request);

    UserDto toDto(User user);
    
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "email",     ignore = true)   // email never changes via update
    @Mapping(target = "active",    ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateUserRequest request, @MappingTarget User user);

    default GenderType mapGender(String gender) {
        if (gender == null || gender.isBlank()) return null;
        try {
            return GenderType.valueOf(gender.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    default String mapGenderToString(GenderType gender) {
        return gender != null ? gender.name() : null;
    }
}