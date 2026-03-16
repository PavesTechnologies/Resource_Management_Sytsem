package com.controller.roleoff_controllers;

import com.dto.UserDTO;
import com.dto.roleoff_dto.RoleOffRequestDTO;
import com.security.CurrentUser;
import com.service_imple.roleoff_service_impl.RoleOffServiceImpl;
import com.service_interface.roleoff_service_interface.RoleOffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/role-off")
@RequiredArgsConstructor
public class RoleOffController {
    private final RoleOffServiceImpl roleOffServiceImpl;
    private final RoleOffService roleOffService;

    @PostMapping
    public ResponseEntity<?> roleOff(
            @RequestBody com.dto.allocation_dto.RoleOffRequestDTO dto,
            @CurrentUser UserDTO userDTO) {

        roleOffServiceImpl.roleOff(dto, userDTO.getId());
        return ResponseEntity.ok("Processed successfully");
    }

    @PostMapping("/{id}/manual-replacement")
    public ResponseEntity<?> manualReplacement(
            @PathVariable UUID id,
            @CurrentUser UserDTO userDTO) {

        roleOffServiceImpl.manualReplacement(id, userDTO.getId());
        return ResponseEntity.ok("Manual replacement created");
    }

    @PostMapping("/role-off-rm")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<?> roleOffByRM(@RequestBody RoleOffRequestDTO roleOff, @CurrentUser UserDTO userDTO) {
        return roleOffService.roleOffByRM(roleOff, userDTO);
    }
}
