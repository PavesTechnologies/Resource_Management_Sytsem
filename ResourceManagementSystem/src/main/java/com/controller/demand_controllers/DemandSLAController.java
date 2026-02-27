package com.controller.demand_controllers;

import com.entity.demand_entities.Demand;
import com.service_interface.demand_service_interface.DemandSLAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/demand-sla")
@CrossOrigin
public class DemandSLAController {

    @Autowired
    private DemandSLAService demandSLAService;

    @GetMapping("/get-all")
    public ResponseEntity<?> getAllDemandSLA() {
        return demandSLAService.getAllDemandSLA();
    }

    @GetMapping("/get-by-demandId/{demandId}")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<?> getDemandSLAByDemandId(@PathVariable UUID demandId) {
        return demandSLAService.getDemandSLAById(demandId);
    }
}
