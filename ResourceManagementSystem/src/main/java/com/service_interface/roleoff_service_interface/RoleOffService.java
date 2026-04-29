package com.service_interface.roleoff_service_interface;

import com.dto.centralised_dto.UserDTO;
import com.dto.roleoff_dto.RoleOffRequestDTO;
import com.dto.roleoff_dto.BulkRoleOffRequestDTO;
import com.dto.resource_dto.ResourceRemovalDTO;
import com.entity.roleoff_entities.RoleOffEvent;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RoleOffService {
    public ResponseEntity<?> roleOffByRM(RoleOffRequestDTO roleOff, UserDTO userDTO);

    /**
     * Get all role-off events with complete details
     */
    List<RoleOffEvent> getAllRoleOffEvents();

    /**
     * Get role-off events by project ID
     */
    List<RoleOffEvent> getRoleOffEventsByProject(Long projectId);

    /**
     * Get role-off events by resource ID
     */
    List<RoleOffEvent> getRoleOffEventsByResource(Long resourceId);

    /**
     * Get role-off event by specific ID
     */
    RoleOffEvent getRoleOffEventById(UUID id);
    public ResponseEntity<?> getResources(Long pmId, Long projectId);
//    public ResponseEntity<?> getRoleOffKPI(Long projectId);
    public ResponseEntity<?> getRoleOffKPI(Long projectId, Long managerId);
    ResponseEntity<?> getRMRoleOffEvents(Long rmId);

    // ========== SEPARATE APPROVE/REJECT METHODS FOR RESOURCE MANAGER ==========

    /**
     * Approve role-off event by Resource Manager
     */
    ResponseEntity<?> rmApprove(UUID id, UserDTO userDTO);

    /**
     * Reject role-off event by Resource Manager with reason
     */
    ResponseEntity<?> rmReject(UUID id, String rejectionReason, UserDTO userDTO);

    // ========== SEPARATE FULFILL/REJECT METHODS FOR DELIVERY MANAGER ==========

    /**
     * Fulfill role-off event by Delivery Manager
     */
    ResponseEntity<?> dlFulfill(UUID id, UserDTO userDTO);

    /**
     * Reject role-off event by Delivery Manager with reason
     */
    ResponseEntity<?> dlReject(UUID id, String rejectionReason, UserDTO userDTO);
    ResponseEntity<?> getDMRoleOffEvents(Long dmId);
    
    // ========== PROJECT MANAGER METHODS ==========
    
    /**
     * Cancel role-off event by Project Manager
     */
    ResponseEntity<?> pmCancel(UUID id, UserDTO userDTO);

    // ========== BULK ROLE-OFF METHODS FOR PLANNED ROLE TYPE ==========

    /**
     * Bulk role-off for planned role type (e.g., project end scenarios)
     */
    ResponseEntity<?> bulkPlannedRoleOff(BulkRoleOffRequestDTO bulkRequest, UserDTO userDTO);

    /**
     * Bulk approve role-off events by Resource Manager
     */
    ResponseEntity<?> bulkRmApprove(List<UUID> ids, UserDTO userDTO);

    /**
     * Bulk reject role-off events by Resource Manager with reason
     */
    ResponseEntity<?> bulkRmReject(List<UUID> ids, String rejectionReason, UserDTO userDTO);

    /**
     * Bulk fulfill role-off events by Delivery Manager
     */
    ResponseEntity<?> bulkDlFulfill(List<UUID> ids, UserDTO userDTO);

    /**
     * Bulk reject role-off events by Delivery Manager with reason
     */
    ResponseEntity<?> bulkDlReject(List<UUID> ids, String rejectionReason, UserDTO userDTO);

    /**
     * Handles attrition for a resource by closing allocations and creating replacements
     */
    void handleAttrition(Long resourceId, LocalDate dateOfExit, Long userId);

    /**
     * Removes a resource from the organization with notice period handling
     */
    String removeResourceFromOrganization(ResourceRemovalDTO removalDTO, Long userId);
}
