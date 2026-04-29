package com.project.authservice;

import com.project.authservice.auth.dto.LoginRequest;
import com.project.authservice.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("auth_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired TestRestTemplate restTemplate;

    @Test
    void registerAndLogin_shouldReturnValidTokens() {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("testuser@travel.com");
        reg.setPassword("Test@1234");
        reg.setFirstName("John");
        reg.setLastName("Doe");

        ResponseEntity<String> registerResp =
                restTemplate.postForEntity("/api/auth/register", reg, String.class);
        assertThat(registerResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        LoginRequest login = new LoginRequest();
        login.setEmail("testuser@travel.com");
        login.setPassword("Test@1234");

        ResponseEntity<String> loginResp =
                restTemplate.postForEntity("/api/auth/login", login, String.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody()).contains("accessToken");
    }

    @Test
    void login_withWrongPassword_shouldReturn401() {
        LoginRequest login = new LoginRequest();
        login.setEmail("nobody@travel.com");
        login.setPassword("WrongPass@1");

        ResponseEntity<String> resp =
                restTemplate.postForEntity("/api/auth/login", login, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
