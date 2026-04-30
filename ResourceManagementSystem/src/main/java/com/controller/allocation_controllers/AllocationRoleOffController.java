package com.controller.allocation_controllers;

import com.dto.centralised_dto.UserDTO;
import com.dto.allocation_dto.RoleOffRequestDTO;
import com.dto.resource_dto.ResourceRemovalDTO;
import com.security.CurrentUser;
import com.service_interface.roleoff_service_interface.RoleOffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

// Force IDE refresh by adding this comment
@RestController
@RequestMapping("/allocation/role-off")
@RequiredArgsConstructor
public class AllocationRoleOffController {
    private final RoleOffService roleOffService;

    @PostMapping
    public ResponseEntity<?> roleOff(
            @RequestBody RoleOffRequestDTO dto,
            @CurrentUser UserDTO userDTO) {

        // 1. Always validate delivery impact and show utilization preview
        String warning = roleOffService.validateDeliveryImpact(dto);
        
        // 2. If not confirmed yet, return preview for user confirmation
        Boolean confirmed = dto.getConfirmed();
        if (confirmed == null || !confirmed) {
            return ResponseEntity.ok(Map.of(
                "requiresConfirmation", true,
                "warning", warning,
                "message", "Please review the role-off impact and confirm to proceed"
            ));
        }
        
        // 3. Log the decision
        roleOffService.logRoleOffDecision(dto, warning, confirmed, userDTO.getId());
        
        // 4. Proceed with role-off after confirmation
        roleOffService.roleOffByRM(dto, userDTO);
        return ResponseEntity.ok("Role-off processed successfully");
    }

    @PostMapping("/{id}/manual-replacement")
    public ResponseEntity<?> manualReplacement(
            @PathVariable UUID id,
            @CurrentUser UserDTO userDTO) {

        roleOffService.manualReplacement(id, userDTO.getId());
        return ResponseEntity.ok("Manual replacement created");
    }

    @PostMapping("/resource-removal")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<?> removeResourceFromOrganization(
            @RequestBody ResourceRemovalDTO removalDTO,
            @CurrentUser UserDTO userDTO) {

        // Set the processed by user
        removalDTO.setProcessedBy(userDTO.getName() + " (" + userDTO.getId() + ")");
        
        // Process the resource removal
        String result = roleOffService.removeResourceFromOrganization(removalDTO, userDTO.getId());
        
        return ResponseEntity.ok(Map.of(
            "message", result,
            "resourceId", removalDTO.getResourceId(),
            "noticePeriodEndDate", removalDTO.getNoticePeriodEndDate(),
            "triggerAttritionImmediately", removalDTO.getTriggerAttritionImmediately()
        ));
    }
}
