package com.controller.allocation_controllers;

import com.dto.ApiResponse;
import com.dto.UserDTO;
import com.dto.allocation_dto.AllocationModificationDecisionDTO;
import com.dto.allocation_dto.AllocationModificationResponseDTO;
import com.dto.allocation_dto.CreateAllocationModificationDTO;
import com.security.CurrentUser;
import com.service_interface.allocation_service_interface.AllocationModificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/allocation-modifications")
@CrossOrigin
public class AllocationModificationController {

    @Autowired
    private AllocationModificationService allocationModificationService;

    @PostMapping("/pm")
    @PreAuthorize("hasRole('PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<?>> createModification(
            @RequestBody CreateAllocationModificationDTO dto,
            @CurrentUser UserDTO userDTO) {
        return allocationModificationService.createModification(dto, userDTO);
    }

    @PutMapping("/{id}/rm/approve")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<?>> approveModification(
            @PathVariable UUID id,
            @CurrentUser UserDTO userDTO) {
        AllocationModificationDecisionDTO decisionDTO = new AllocationModificationDecisionDTO();
        decisionDTO.setDecision("APPROVE");
        return allocationModificationService.processModificationDecision(id, decisionDTO, userDTO);
    }

    @PutMapping("/{id}/rm/reject")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<?>> rejectModification(
            @PathVariable UUID id,
            @RequestBody AllocationModificationDecisionDTO dto,
            @CurrentUser UserDTO userDTO) {
        dto.setDecision("REJECT");
        return allocationModificationService.processModificationDecision(id, dto, userDTO);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<AllocationModificationResponseDTO>> getModificationById(
            @PathVariable UUID id) {
        return allocationModificationService.getModificationById(id);
    }

    @GetMapping("/demand/{demandId}")
    @PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<List<AllocationModificationResponseDTO>>> getModificationsByDemand(
            @PathVariable UUID demandId) {
        return allocationModificationService.getModificationsByDemand(demandId);
    }

    @DeleteMapping("/{id}/pm")
    @PreAuthorize("hasRole('PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<?>> deleteModification(
            @PathVariable UUID id,
            @CurrentUser UserDTO userDTO) {
        return allocationModificationService.deleteModification(id, userDTO);
    }
}
