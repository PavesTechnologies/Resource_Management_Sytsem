package com.controller.allocation_controllers;

import com.dto.UserDTO;
import com.dto.allocation_dto.RoleOffRequestDTO;
import com.entity_enums.allocation_enums.RoleOffType;
import com.entity_enums.allocation_enums.RoleOffReason;
import com.security.CurrentUser;
import com.service_imple.allocation_service_imple.RoleOffServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

// Force IDE refresh by adding this comment
@RestController
@RequestMapping("/role-off")
@RequiredArgsConstructor
public class RoleOffController {
    private final RoleOffServiceImpl roleOffService;

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
        roleOffService.roleOff(dto, userDTO.getId());
        return ResponseEntity.ok("Role-off processed successfully");
    }

    @PostMapping("/{id}/manual-replacement")
    public ResponseEntity<?> manualReplacement(
            @PathVariable UUID id,
            @CurrentUser UserDTO userDTO) {

        roleOffService.manualReplacement(id, userDTO.getId());
        return ResponseEntity.ok("Manual replacement created");
    }
}
