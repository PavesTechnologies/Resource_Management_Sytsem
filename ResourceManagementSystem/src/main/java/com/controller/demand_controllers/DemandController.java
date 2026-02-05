package com.controller.demand_controllers;

import com.dto.ApiResponse;
import com.entity.Demand;
import com.service_interface.demand_service_interface.DemandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demand")
@CrossOrigin
public class DemandController {

    @Autowired
    private DemandService demandService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','PROJECT-MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse> createDemand(@RequestBody Demand demand) {
        return demandService.createDemand(demand);
    }
}
