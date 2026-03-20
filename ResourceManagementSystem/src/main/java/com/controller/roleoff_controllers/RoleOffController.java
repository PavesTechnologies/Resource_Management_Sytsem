package com.controller.roleoff_controllers;

import com.dto.UserDTO;
import com.dto.roleoff_dto.RoleOffRequestDTO;
import com.entity.allocation_entities.RoleOffEvent;
import com.entity_enums.allocation_enums.RoleOffStatus;
import com.security.CurrentUser;
import com.service_imple.roleoff_service_impl.RoleOffServiceImpl;
import com.service_interface.roleoff_service_interface.RoleOffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/role-off")
@RequiredArgsConstructor
public class RoleOffController {
    private final RoleOffServiceImpl roleOffServiceImpl;
    private final RoleOffService roleOffService;

    @PostMapping
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER', 'ADMIN')")
    public ResponseEntity<?> roleOff(
            @RequestBody com.dto.allocation_dto.RoleOffRequestDTO dto,
            @CurrentUser UserDTO userDTO)
    {

        roleOffServiceImpl.roleOff(dto, userDTO.getId());
        return ResponseEntity.ok("Processed successfully");
    }

    @PostMapping("/{id}/manual-replacement")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER', 'ADMIN')")
    public ResponseEntity<?> manualReplacement(
            @PathVariable UUID id,
            @CurrentUser UserDTO userDTO) {

        roleOffServiceImpl.manualReplacement(id, userDTO.getId());
        return ResponseEntity.ok("Manual replacement created");
    }

    @PostMapping("/{id}/rm-action")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<?> rmAction(
            @PathVariable UUID id,
            @RequestParam boolean approve,
            @RequestParam(required = false) String comments,
            @CurrentUser UserDTO userDTO) {

        return roleOffServiceImpl.rmAction(id, approve, comments, userDTO);
    }

    @PostMapping("/{id}/dl-action")
    @PreAuthorize("hasRole('DELIVERY-MANAGER')")
    public ResponseEntity<?> dlAction(
            @PathVariable UUID id,
            @RequestParam RoleOffStatus action,
            @RequestParam(required = false) String comments,
            @CurrentUser UserDTO userDTO) {

        return roleOffServiceImpl.dlAction(id, action, comments, userDTO);
    }

    @PostMapping("/role-off-rm")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<?> roleOffByRM(@RequestBody RoleOffRequestDTO roleOff, @CurrentUser UserDTO userDTO) {
        return roleOffService.roleOffByRM(roleOff, userDTO);
    }

    @GetMapping("/get-resources/{projectId}")
    @PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'RESOURCE-MANAGER')")
    public ResponseEntity<?> getResources(@CurrentUser UserDTO userDTO, @PathVariable Long projectId) {
        return roleOffService.getResources(userDTO, projectId);
    }

    // ========== GET ENDPOINTS FOR ROLE-OFF DETAILS ==========

    /**
     * Get all role-off events with complete details
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER', 'ADMIN', 'PROJECT-MANAGER')")
    public ResponseEntity<List<RoleOffEvent>> getAllRoleOffEvents() {
        List<RoleOffEvent> events = roleOffService.getAllRoleOffEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * Get role-off events by project ID
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER', 'ADMIN')")
    public ResponseEntity<List<RoleOffEvent>> getRoleOffEventsByProject(@PathVariable Long projectId) {
        List<RoleOffEvent> events = roleOffService.getRoleOffEventsByProject(projectId);
        return ResponseEntity.ok(events);
    }

    /**
     * Get role-off events by resource ID
     */
    @GetMapping("/resource/{resourceId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER', 'ADMIN')")
    public ResponseEntity<List<RoleOffEvent>> getRoleOffEventsByResource(@PathVariable Long resourceId) {
        List<RoleOffEvent> events = roleOffService.getRoleOffEventsByResource(resourceId);
        return ResponseEntity.ok(events);
    }

    /**
     * Get role-off event by specific ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER', 'ADMIN')")
    public ResponseEntity<RoleOffEvent> getRoleOffEventById(@PathVariable UUID id) {
        RoleOffEvent event = roleOffService.getRoleOffEventById(id);
        return ResponseEntity.ok(event);
    }
}
