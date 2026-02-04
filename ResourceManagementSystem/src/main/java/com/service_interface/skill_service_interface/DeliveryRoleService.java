package com.service_interface.skill_service_interface;

import com.entity.skill_entities.DeliveryRole;
import com.dto.UserDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRoleService {

    DeliveryRole createRole(
            String roleName,
            String roleDescription,
            UserDTO user
    );

    void deactivateRole(UUID roleId, UserDTO user);

    Optional<DeliveryRole> getActiveRole(UUID roleId);

    List<DeliveryRole> getAllActiveRoles();
}
