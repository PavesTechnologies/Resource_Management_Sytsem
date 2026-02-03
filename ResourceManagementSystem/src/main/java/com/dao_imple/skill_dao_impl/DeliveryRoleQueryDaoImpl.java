package com.dao_imple.skill_dao_impl;

import com.dao_interface.skill_dao.DeliveryRoleQueryDao;
import com.entity.skill_entities.DeliveryRole;
import com.repo.skill_repo.DeliveryRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryRoleQueryDaoImpl implements DeliveryRoleQueryDao {

    private final DeliveryRoleRepository roleRepo;

    @Override
    public Optional<DeliveryRole> findActiveRoleByName(String roleName) {
        return roleRepo.findByRoleNameIgnoreCase(roleName)
                .filter(DeliveryRole::getActiveFlag);
    }

    @Override
    public Optional<DeliveryRole> findActiveRoleById(UUID roleId) {
        return roleRepo.findById(roleId)
                .filter(DeliveryRole::getActiveFlag);
    }
}
