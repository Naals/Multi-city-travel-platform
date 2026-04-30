package com.project.userservice;

import com.project.userservice.dto.CreateUserRequest;
import com.project.userservice.dto.UserDto;
import com.project.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
class UserServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("user_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired UserService userService;

    @Test
    void createUser_thenFindById_shouldReturnCorrectProfile() {
        UUID id = UUID.randomUUID();
        CreateUserRequest req = new CreateUserRequest();
        req.setUserId(id);
        req.setFirstName("Alice");
        req.setLastName("Smith");
        req.setEmail("alice@travel.com");

        UserDto created = userService.createUser(req);
        assertThat(created.getId()).isEqualTo(id);
        assertThat(created.getEmail()).isEqualTo("alice@travel.com");

        UserDto fetched = userService.getUserById(id);
        assertThat(fetched.getFirstName()).isEqualTo("Alice");
    }
}
