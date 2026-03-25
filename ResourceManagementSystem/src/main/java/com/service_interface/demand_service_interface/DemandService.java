package com.service_interface.demand_service_interface;

import com.dto.centralised_dto.ApiResponse;
import com.dto.centralised_dto.UserDTO;
import com.dto.demand_dto.*;
import com.security.CurrentUser;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface DemandService {

    ResponseEntity<ApiResponse<?>> updateDemand(UpdateDemandDTO dto);
    ResponseEntity<ApiResponse<?>> getDemandByProjectId(Long projectId);

    ResponseEntity<ApiResponse<?>> createDemand(CreateDemandDTO dto, Long id);
    ResponseEntity<ApiResponse<?>> getDemandById(UUID demandId);
    ResponseEntity<ApiResponse<?>> getDemandKpiByResourceManagerId(Long resourceManagerId);
    ResponseEntity<ApiResponse<?>> getDashboardKpi(Long projectId);
    ResponseEntity<ApiResponse<?>> getDemandsByResourceManagerId(Long resourceManagerId);
    ResponseEntity<ApiResponse<?>> getDemandsByCreatedBy(Long createdBy);
    ResponseEntity<ApiResponse<?>> getDemandsByCreatedByAndProjectId(Long createdBy, Long projectId);

    ResponseEntity<ApiResponse<?>> deleteDemand(UUID demandId, UserDTO userDTO);
    
    // Conflict resolution method
    ResponseEntity<ApiResponse<?>> resolveDemandConflicts(Long projectId);
    
    // Pre-submission validation for early conflict detection
    ResponseEntity<ApiResponse<DemandConflictValidationDTO>> validateDemandConflicts(CreateDemandDTO dto);
    
    // Delivery Manager KPI endpoint (using token-based user ID)
    ResponseEntity<ApiResponse<DemandKpiDTO>> getDeliveryManagerKpi(@CurrentUser UserDTO userDTO);
    
    // Delivery Manager demand details endpoint (using token-based user ID)
    ResponseEntity<ApiResponse<List<DeliveryManagerDemandDTO>>> getDeliveryManagerDemandDetails(@CurrentUser UserDTO userDTO);
    ResponseEntity<ApiResponse<?>> processDemandDecision(DemandDecisionDTO dto);
    ResponseEntity<ApiResponse<?>> processResourceManagerDecision(DemandDecisionDTO dto, UserDTO userDTO);
}
