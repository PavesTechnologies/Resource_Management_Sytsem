package com.service_interface.roleoff_service_interface;

import com.dto.UserDTO;
import com.dto.roleoff_dto.RoleOffRequestDTO;
import com.entity.allocation_entities.RoleOffEvent;
import org.springframework.http.ResponseEntity;

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
    public ResponseEntity<?> getRoleOffKPI(Long projectId);
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
}
