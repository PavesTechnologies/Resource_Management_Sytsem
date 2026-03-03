package com.controller.demand_controllers;

import com.dto.ApiResponse;
import com.dto.UserDTO;
import com.dto.demand_dto.CreateDemandDTO;
import com.dto.demand_dto.DemandConflictValidationDTO;
import com.dto.demand_dto.UpdateDemandDTO;
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
    public ResponseEntity<ApiResponse<?>> createDemand(
            @RequestBody CreateDemandDTO dto,
            @CurrentUser UserDTO userDTO) {

        return demandService.createDemand(dto, userDTO.getId());
    }

    // Pre-submission validation endpoint for early conflict detection
    @PostMapping("/validate-conflicts")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','PROJECT-MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<DemandConflictValidationDTO>> validateDemandConflicts(
            @RequestBody CreateDemandDTO dto) {
        return demandService.validateDemandConflicts(dto);
    }

    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','PROJECT-MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<?>> updateDemand(@RequestBody UpdateDemandDTO dto) {
        return demandService.updateDemand(dto);
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

    @GetMapping("/demands")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','PROJECT-MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<?>> getDemandsByResourceManagerProjects(@CurrentUser UserDTO userDTO) {
        return demandService.getDemandsByResourceManagerId(userDTO.getId());
    }

    @GetMapping("/kpi")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','PROJECT-MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<?>> getDemandKpiByResourceManagerProjects(@CurrentUser UserDTO userDTO) {
        return demandService.getDemandKpiByResourceManagerId(userDTO.getId());
    }

    @GetMapping("/dashboard-kpi")
    @PreAuthorize("hasRole('PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<?>> getDashboardKpi(@CurrentUser UserDTO userDTO) {
        return demandService.getDashboardKpi(userDTO);
    }

    // Conflict resolution endpoint
    @PostMapping("/resolve-conflicts/{projectId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','PROJECT-MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<?>> resolveDemandConflicts(@PathVariable Long projectId) {
        return demandService.resolveDemandConflicts(projectId);
    }
}
