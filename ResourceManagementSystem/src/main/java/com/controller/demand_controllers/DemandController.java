package com.controller.demand_controllers;

import com.dto.ApiResponse;
import com.dto.UserDTO;
import com.dto.demand_dto.*;
import com.security.CurrentUser;
import com.service_interface.demand_service_interface.DemandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/demand")
@CrossOrigin
public class DemandController {

    @Autowired
    private DemandService demandService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<?>> createDemand(
            @RequestBody CreateDemandDTO dto,
            @CurrentUser UserDTO userDTO) {

        return demandService.createDemand(dto, userDTO.getId());
    }

    @PutMapping("/update/pm")
    @PreAuthorize("hasRole('PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<?>> updateDemandByPM(@RequestBody UpdateDemandDTO dto) {
        return demandService.updateDemand(dto);
    }

    @DeleteMapping("/delete/pm/{demandId}")
    @PreAuthorize("hasRole('PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<?>> deleteDemandByPM(
            @PathVariable UUID demandId,
            @CurrentUser UserDTO userDTO) {
        return demandService.deleteDemand(demandId, userDTO);
    }


    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getDemandByProjectId(@PathVariable Long projectId) {
        return demandService.getDemandByProjectId(projectId);
    }

    @GetMapping("/{demandId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','PROJECT-MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<?>> getDemandById(@PathVariable UUID demandId) {
        return demandService.getDemandById(demandId);
    }

    @GetMapping("/rm/demands")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<?>> getDemandsByResourceManagerProjects(@CurrentUser UserDTO userDTO) {
        return demandService.getDemandsByResourceManagerId(userDTO.getId());
    }

    @GetMapping("/rm/kpi")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<?>> getDemandKpiByResourceManagerProjects(@CurrentUser UserDTO userDTO) {
        return demandService.getDemandKpiByResourceManagerId(userDTO.getId());
    }

    @GetMapping("/pm/kpi")
    @PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getDashboardKpi(@CurrentUser UserDTO userDTO) {
        return demandService.getDashboardKpi(userDTO);
    }

    // Delivery Manager KPI endpoint (uses token-based authentication)
    @GetMapping("/dm/kpi")
    @PreAuthorize("hasRole('DELIVERY-MANAGER')")
    public ResponseEntity<ApiResponse<DemandKpiDTO>> getDeliveryManagerKpi(@CurrentUser UserDTO userDTO) {
        return demandService.getDeliveryManagerKpi(userDTO);
    }

    // Delivery Manager demand details endpoint (uses token-based authentication)
    @GetMapping("/dm/demands")
    @PreAuthorize("hasRole('DELIVERY-MANAGER')")
    public ResponseEntity<ApiResponse<List<DeliveryManagerDemandDTO>>> getDeliveryManagerDemandDetails(@CurrentUser UserDTO userDTO) {
        return demandService.getDeliveryManagerDemandDetails(userDTO);
    }

    @PutMapping("/dm/decision")
    @PreAuthorize("hasRole('DELIVERY-MANAGER')")
    public ResponseEntity<ApiResponse<?>> processDemandDecision(
            @RequestBody DemandDecisionDTO dto,
            @CurrentUser UserDTO userDTO) {

        return demandService.processDemandDecision(dto);
    }

    @PutMapping("/rm/decision")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<?>> processResourceManagerDecision(
            @RequestBody DemandDecisionDTO dto,
            @CurrentUser UserDTO userDTO) {

        return demandService.processResourceManagerDecision(dto, userDTO);
    }
}
