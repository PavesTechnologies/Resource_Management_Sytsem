package com.dao_interface.skill_dao;

import com.entity.skill_entities.DeliveryRole;

import java.util.Optional;

public interface DeliveryRoleQueryDao {

    Optional<DeliveryRole> findActiveRoleByName(String roleName);

    Optional<DeliveryRole> findActiveRoleById(Long roleId);
}
