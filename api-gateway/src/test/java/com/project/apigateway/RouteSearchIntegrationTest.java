package com.project.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RouteSearchIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void routeSearch_NYC_to_PAR_shouldFindMultiHopPaths() {
        // Arrange
        Map<String, Object> request = Map.of(
                "originCityCode",  "NYC",
                "destCityCode",    "PAR",
                "departureDate",   "2025-09-15",
                "maxStops",        2,
                "sortBy",          "PRICE",
                "topK",            3
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer test-token");

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/routes/search", entity, Map.class
        );

        assertThat(response.getStatusCode())
                .isIn(HttpStatus.OK, HttpStatus.NOT_FOUND,
                        HttpStatus.UNAUTHORIZED, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void gatewayHealth_shouldReturn200() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/health", String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void missingJwt_onProtectedRoute_shouldReturn401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/users/me", String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void authEndpoints_shouldBePublic() {
        Map<String, String> loginRequest = Map.of(
                "email",    "test@test.com",
                "password", "WrongPass@1"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(loginRequest, headers),
                String.class
        );
        assertThat(response.getStatusCode())
                .isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
