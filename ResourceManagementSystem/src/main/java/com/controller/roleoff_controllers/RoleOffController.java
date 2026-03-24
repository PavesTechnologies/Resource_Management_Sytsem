package com.controller.roleoff_controllers;

import com.dto.UserDTO;
import com.dto.roleoff_dto.RoleOffRequestDTO;
import com.entity.allocation_entities.RoleOffEvent;
import com.entity_enums.allocation_enums.RoleOffReason;
import com.entity_enums.allocation_enums.RoleOffStatus;
import com.security.CurrentUser;
import com.service_imple.roleoff_service_impl.RoleOffServiceImpl;
import com.service_interface.roleoff_service_interface.RoleOffService;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
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
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER', 'ADMIN', 'PROJECT-MANAGER')")
    public ResponseEntity<?> roleOff(
            @RequestBody com.dto.allocation_dto.RoleOffRequestDTO dto,
            @CurrentUser UserDTO userDTO)
    {

        return roleOffServiceImpl.roleOff(dto, userDTO.getId());
    }

    @PostMapping("/{id}/manual-replacement")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER', 'ADMIN')")
    public ResponseEntity<?> manualReplacement(
            @PathVariable UUID id,
            @CurrentUser UserDTO userDTO) {

        roleOffServiceImpl.manualReplacement(id, userDTO.getId());
        return ResponseEntity.ok("Manual replacement created");
    }

    // ========== SEPARATE RESOURCE MANAGER ENDPOINTS ==========

    @PostMapping("/{id}/rm-approve")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<?> rmApprove(
            @PathVariable UUID id,
            @CurrentUser UserDTO userDTO) {
        return roleOffService.rmApprove(id, userDTO);
    }

    @PostMapping("/{id}/rm-reject")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<?> rmReject(
            @PathVariable UUID id,
            @RequestParam String rejectionReason,
            @CurrentUser UserDTO userDTO) {
        return roleOffService.rmReject(id, rejectionReason, userDTO);
    }

    // ========== SEPARATE DELIVERY MANAGER ENDPOINTS ==========

    @PostMapping("/{id}/dl-fulfill")
    @PreAuthorize("hasRole('DELIVERY-MANAGER')")
    public ResponseEntity<?> dlFulfill(
            @PathVariable UUID id,
            @CurrentUser UserDTO userDTO) {
        return roleOffService.dlFulfill(id, userDTO);
    }

    @PostMapping("/{id}/dl-reject")
    @PreAuthorize("hasRole('DELIVERY-MANAGER')")
    public ResponseEntity<?> dlReject(
            @PathVariable UUID id,
            @RequestParam String rejectionReason,
            @CurrentUser UserDTO userDTO) {
        return roleOffService.dlReject(id, rejectionReason, userDTO);
    }

    // ========== PROJECT MANAGER ENDPOINTS ==========

    @PostMapping("/{id}/pm-cancel")
    @PreAuthorize("hasRole('PROJECT-MANAGER')")
    public ResponseEntity<?> pmCancel(
            @PathVariable UUID id,
            @CurrentUser UserDTO userDTO) {
        return roleOffService.pmCancel(id, userDTO);
    }

    @PostMapping("/role-off-rm")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<?> roleOffByRM(@RequestBody RoleOffRequestDTO roleOff, @CurrentUser UserDTO userDTO) {
        return roleOffService.roleOffByRM(roleOff, userDTO);
    }

    @GetMapping("/get-resources/{projectId}")
    @PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'RESOURCE-MANAGER')")
    public ResponseEntity<?> getResources(@CurrentUser UserDTO userDTO, @PathVariable Long projectId) {
        return roleOffService.getResources(userDTO.getId(), projectId);
    }

    @GetMapping("/get-role-off-project-kpi/{projectId}")
    public ResponseEntity<?> getRoleOffProjectKPI(@PathVariable Long projectId) {
        return roleOffService.getRoleOffKPI(projectId);
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

    @GetMapping("/get-role-off-rm")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<?> getRoleOffEventsRM(@CurrentUser UserDTO userDTO) {
        return roleOffService.getRMRoleOffEvents(userDTO.getId());
    }

    @GetMapping("/get-role-off-dm")
    @PreAuthorize("hasRole('DELIVERY-MANAGER')")
    public ResponseEntity<?> getRoleOffEventsDM(@CurrentUser UserDTO userDTO) {
        return roleOffService.getDMRoleOffEvents(userDTO.getId());
    }

    /**
     * Get all role-off reason enum values
     */
    @GetMapping("/reasons")
    public ResponseEntity<List<RoleOffReason>> getRoleOffReasons() {
        List<RoleOffReason> reasons = List.of(RoleOffReason.values());
        return ResponseEntity.ok(reasons);
    }
}
