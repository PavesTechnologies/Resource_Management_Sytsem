package com.controller.demand_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.entity.demand_entities.Demand;
import com.service_interface.demand_service_interface.DemandSLAService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/demand-sla")
@RequiredArgsConstructor
public class DemandSLAController {

    private final DemandSLAService demandSLAService;

    @GetMapping("/get-all")
    public ResponseEntity<ApiResponse<?>> getAllDemandSLA() {
        return demandSLAService.getAllDemandSLA();
    }

    @GetMapping("/get-by-demandId/{demandId}")
    @PreAuthorize("hasRole('Resource_Manager')")
    public ResponseEntity<ApiResponse<?>> getDemandSLAByDemandId(@PathVariable UUID demandId) {
        return demandSLAService.getDemandSLAById(demandId);
    }
}
