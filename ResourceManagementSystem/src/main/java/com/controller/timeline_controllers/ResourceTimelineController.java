package com.controller.timeline_controllers;

import com.dto.ResourceTimelineDTO;
import com.service_interface.timeline_interface.ResourceTimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resource-timeline")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Resource Timeline API", description = "API for retrieving resource timeline and allocation information")
public class ResourceTimelineController {

    private final ResourceTimelineService resourceTimelineService;

    @GetMapping
    @Operation(
        summary = "Get all resource timelines",
        description = "Retrieves a comprehensive timeline view for all active resources including current allocations, utilization history, and future assignments. The API is fully dynamic and works correctly with zero or many records using only aggregation-based calculations."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved resource timelines",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ResourceTimelineDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<List<ResourceTimelineDTO>> getAllResourceTimelines() {
        log.info("Fetching all resource timelines");
        List<ResourceTimelineDTO> timelines = resourceTimelineService.getAllResourceTimelines();
        log.info("Successfully retrieved {} resource timelines", timelines.size());
        return ResponseEntity.ok(timelines);
    }

    @GetMapping("/health")
    @Operation(
        summary = "Health check endpoint",
        description = "Simple health check to verify the timeline API is accessible"
    )
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Resource Timeline API is operational");
    }
}
