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
    public ResponseEntity<?> getResources(UserDTO userDTO, Long projectId);
}
