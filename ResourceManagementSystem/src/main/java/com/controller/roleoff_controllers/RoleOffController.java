package com.controller.roleoff_controllers;

import com.dto.allocation_dto.RoleOffRequestDTO;
import com.dto.centralised_dto.UserDTO;
import com.dto.roleoff_dto.BulkRoleOffRequestDTO;
import com.entity.roleoff_entities.RoleOffEvent;
import com.entity_enums.roleoff_enums.RoleOffReason;
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
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin', 'Project_Manager')")
    public ResponseEntity<?> roleOff(
            @RequestBody com.dto.allocation_dto.RoleOffRequestDTO dto,
            @CurrentUser UserDTO userDTO)
    {

        return roleOffServiceImpl.roleOffByRM(dto, userDTO);
    }

    @PostMapping("/{id}/manual-replacement")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin')")
    public ResponseEntity<?> manualReplacement(
            @PathVariable UUID id,
            @CurrentUser UserDTO userDTO) {

        roleOffServiceImpl.manualReplacement(id, userDTO.getId());
        return ResponseEntity.ok("Manual replacement created");
    }

    // ========== SEPARATE RESOURCE MANAGER ENDPOINTS ==========

    @PostMapping("/{id}/rm-approve")
    @PreAuthorize("hasRole('Resource_Manager')")
    public ResponseEntity<?> rmApprove(
            @PathVariable UUID id,
            @CurrentUser UserDTO userDTO) {
        return roleOffService.rmApprove(id, userDTO);
    }

    @PostMapping("/{id}/rm-reject")
    @PreAuthorize("hasRole('Resource_Manager')")
    public ResponseEntity<?> rmReject(
            @PathVariable UUID id,
            @RequestParam String rejectionReason,
            @CurrentUser UserDTO userDTO) {
        return roleOffService.rmReject(id, rejectionReason, userDTO);
    }

    // ========== SEPARATE DELIVERY MANAGER ENDPOINTS ==========

    @PostMapping("/{id}/dl-fulfill")
    @PreAuthorize("hasRole('Delivery_Manager')")
    public ResponseEntity<?> dlFulfill(
            @PathVariable UUID id,
            @CurrentUser UserDTO userDTO) {
        return roleOffService.dlFulfill(id, userDTO);
    }

    @PostMapping("/{id}/dl-reject")
    @PreAuthorize("hasRole('Delivery_Manager')")
    public ResponseEntity<?> dlReject(
            @PathVariable UUID id,
            @RequestParam String rejectionReason,
            @CurrentUser UserDTO userDTO) {
        return roleOffService.dlReject(id, rejectionReason, userDTO);
    }

    // ========== PROJECT MANAGER ENDPOINTS ==========

    @PostMapping("/{id}/pm-cancel")
    @PreAuthorize("hasRole('Project_Manager')")
    public ResponseEntity<?> pmCancel(
            @PathVariable UUID id,
            @CurrentUser UserDTO userDTO) {
        return roleOffService.pmCancel(id, userDTO);
    }

    @PostMapping("/role-off-rm")
    @PreAuthorize("hasRole('Resource_Manager')")
    public ResponseEntity<?> roleOffByRM(@RequestBody RoleOffRequestDTO roleOff, @CurrentUser UserDTO userDTO) {
        return roleOffService.roleOffByRM(roleOff, userDTO);
    }

    @GetMapping("/get-resources/{projectId}")
    @PreAuthorize("hasAnyRole('Project_Manager', 'Resource_Manager')")
    public ResponseEntity<?> getResources(@CurrentUser UserDTO userDTO, @PathVariable Long projectId) {
        return roleOffService.getResources(userDTO.getId(), projectId);
    }

    @GetMapping("/get-role-off-project-kpi/{projectId}")
    public ResponseEntity<?> getRoleOffProjectKPI(@PathVariable Long projectId, @CurrentUser UserDTO userDTO) {
        return roleOffService.getRoleOffKPI(projectId, userDTO.getId());
    }

    // ========== GET ENDPOINTS FOR ROLE-OFF DETAILS ==========

    /**
     * Get all role-off events with complete details
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin', 'Project_Manager')")
    public ResponseEntity<List<RoleOffEvent>> getAllRoleOffEvents() {
        List<RoleOffEvent> events = roleOffService.getAllRoleOffEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * Get role-off events by project ID
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin')")
    public ResponseEntity<List<RoleOffEvent>> getRoleOffEventsByProject(@PathVariable Long projectId) {
        List<RoleOffEvent> events = roleOffService.getRoleOffEventsByProject(projectId);
        return ResponseEntity.ok(events);
    }

    /**
     * Get role-offs pending delivery manager action
     */
    @GetMapping("/pending-dm-action")
    @PreAuthorize("hasRole('Delivery_Manager')")
    public ResponseEntity<?> getRoleOffsPendingDMAction(@CurrentUser UserDTO userDTO) {
        return roleOffService.getDMRoleOffEvents(userDTO.getId());
    }

    /**
     * Get fulfilled role-offs for delivery manager
     */
    @GetMapping("/fulfilled-dm-action")
    @PreAuthorize("hasRole('Delivery_Manager')")
    public ResponseEntity<?> getFulfilledRoleOffsForDM(
            @CurrentUser UserDTO userDTO) {

        return roleOffService.getFulfilledRoleOffEvents(userDTO.getId());
    }

    /**
     * Get role-offs approved today by delivery manager for KPI tracking
     */
    @GetMapping("/approved-today")
    @PreAuthorize("hasAnyRole('Delivery_Manager', 'Resource_Manager', 'Admin')")
    public ResponseEntity<?> getRoleOffsApprovedToday(
            @RequestParam(required = false) Long projectId,
            @CurrentUser UserDTO userDTO) {
        return roleOffService.getRoleOffsApprovedToday(projectId, userDTO.getId());
    }

    /**
     * Get role-off event by specific ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin')")
    public ResponseEntity<RoleOffEvent> getRoleOffEventById(@PathVariable UUID id) {
        RoleOffEvent event = roleOffService.getRoleOffEventById(id);
        return ResponseEntity.ok(event);
    }

    /**
     * Get role-off events by resource ID
     */
    @GetMapping("/resource/{resourceId}")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin')")
    public ResponseEntity<List<RoleOffEvent>> getRoleOffEventsByResource(@PathVariable String resourceId) {
        List<RoleOffEvent> events = roleOffService.getRoleOffEventsByResource(resourceId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/get-role-off-rm")
    @PreAuthorize("hasRole('Resource_Manager')")
    public ResponseEntity<?> getRoleOffEventsRM(@CurrentUser UserDTO userDTO) {
        return roleOffService.getRMRoleOffEvents(userDTO.getId());
    }

    @GetMapping("/get-role-off-dm")
    @PreAuthorize("hasRole('Delivery_Manager')")
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

    // ========== BULK ROLE-OFF ENDPOINTS FOR PLANNED ROLE TYPE ==========

    @PostMapping("/bulk-planned")
    @PreAuthorize("hasAnyRole('Project_Manager', 'Resource_Manager')")
    public ResponseEntity<?> bulkPlannedRoleOff(
            @RequestBody BulkRoleOffRequestDTO bulkRequest,
            @CurrentUser UserDTO userDTO) {
        return roleOffService.bulkPlannedRoleOff(bulkRequest, userDTO);
    }

    @PostMapping("/bulk-rm-approve")
    @PreAuthorize("hasRole('Resource_Manager')")
    public ResponseEntity<?> bulkRmApprove(
            @RequestBody List<UUID> ids,
            @CurrentUser UserDTO userDTO) {
        return roleOffService.bulkRmApprove(ids, userDTO);
    }

    @PostMapping("/bulk-rm-reject")
    @PreAuthorize("hasRole('Resource_Manager')")
    public ResponseEntity<?> bulkRmReject(
            @RequestBody List<UUID> ids,
            @RequestParam String rejectionReason,
            @CurrentUser UserDTO userDTO) {
        return roleOffService.bulkRmReject(ids, rejectionReason, userDTO);
    }

    @PostMapping("/bulk-dl-fulfill")
    @PreAuthorize("hasRole('Delivery_Manager')")
    public ResponseEntity<?> bulkDlFulfill(
            @RequestBody List<UUID> ids,
            @CurrentUser UserDTO userDTO) {
        return roleOffService.bulkDlFulfill(ids, userDTO);
    }

    @PostMapping("/bulk-dl-reject")
    @PreAuthorize("hasRole('Delivery_Manager')")
    public ResponseEntity<?> bulkDlReject(
            @RequestBody List<UUID> ids,
            @RequestParam String rejectionReason,
            @CurrentUser UserDTO userDTO) {
        return roleOffService.bulkDlReject(ids, rejectionReason, userDTO);
    }
}
