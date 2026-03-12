package com.controller.allocation_controllers;

import com.dto.UserDTO;
import com.dto.allocation_dto.RoleOffRequestDTO;
import com.security.CurrentUser;
import com.service_imple.allocation_service_imple.RoleOffServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/role-off")
@RequiredArgsConstructor
public class RoleOffController {
    private final RoleOffServiceImpl roleOffService;

    @PostMapping
    public ResponseEntity<?> roleOff(
            @RequestBody RoleOffRequestDTO dto,
            @CurrentUser UserDTO userDTO) {

        roleOffService.roleOff(dto, userDTO.getId());
        return ResponseEntity.ok("Processed successfully");
    }

    @PostMapping("/{id}/manual-replacement")
    public ResponseEntity<?> manualReplacement(
            @PathVariable UUID id,
            @CurrentUser UserDTO userDTO) {

        roleOffService.manualReplacement(id, userDTO.getId());
        return ResponseEntity.ok("Manual replacement created");
    }
}
