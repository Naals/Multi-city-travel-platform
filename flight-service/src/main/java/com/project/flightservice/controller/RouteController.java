package com.project.flightservice.controller;

import com.project.flightservice.dto.*;
import com.project.flightservice.service.RouteSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Tag(name = "Route Search", description = "Dijkstra multi-city flight route search")
public class RouteController {

    private final RouteSearchService routeSearchService;

    @PostMapping("/search")
    @Operation(summary = "Search multi-city routes using Dijkstra's algorithm")
    public ResponseEntity<RouteSearchResponse> search(
            @Valid @RequestBody RouteSearchRequest request) {
        return ResponseEntity.ok(routeSearchService.search(request));
    }
}
