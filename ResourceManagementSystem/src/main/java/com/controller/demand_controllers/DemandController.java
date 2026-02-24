package com.controller.demand_controllers;

import com.dto.ApiResponse;
import com.dto.UserDTO;
import com.entity.demand_entities.Demand;
import com.security.CurrentUser;
import com.service_interface.demand_service_interface.DemandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/demand")
@CrossOrigin
public class DemandController {

    @Autowired
    private DemandService demandService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','PROJECT-MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<?>> createDemand(@RequestBody Demand demand, @CurrentUser UserDTO userDTO) {
        demand.setCreatedBy(userDTO.getId());
        return demandService.createDemand(demand);
    }

    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','PROJECT-MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<?>> updateDemand(@RequestBody Demand demand) {
        return demandService.updateDemand(demand);
    }


    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','PROJECT-MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<?>> getDemandByProjectId(@PathVariable Long projectId) {
        return demandService.getDemandByProjectId(projectId);
    }

    @GetMapping("/{demandId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','PROJECT-MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<?>> getDemandById(@PathVariable UUID demandId) {
        return demandService.getDemandById(demandId);
    }
}
