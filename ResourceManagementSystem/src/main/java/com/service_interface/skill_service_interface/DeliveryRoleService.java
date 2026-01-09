package com.service_interface.skill_service_interface;

import com.entity.skill_entities.DeliveryRole;
import com.dto.UserDTO;

import java.util.List;
import java.util.Optional;

public interface DeliveryRoleService {

    DeliveryRole createRole(
            String roleName,
            String roleDescription,
            UserDTO user
    );

    void deactivateRole(Long roleId, UserDTO user);

    Optional<DeliveryRole> getActiveRole(Long roleId);

    List<DeliveryRole> getAllActiveRoles();
}
