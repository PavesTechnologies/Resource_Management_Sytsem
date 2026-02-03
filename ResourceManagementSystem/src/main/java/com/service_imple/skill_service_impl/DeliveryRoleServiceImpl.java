package com.service_imple.skill_service_impl;

import com.dto.UserDTO;
import com.entity.skill_entities.DeliveryRole;
import com.repo.skill_repo.DeliveryRoleRepository;
import com.service_interface.skill_service_interface.SkillAuditService;
import com.service_interface.skill_service_interface.DeliveryRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryRoleServiceImpl implements DeliveryRoleService {

    private final DeliveryRoleRepository roleRepo;
    private final SkillAuditService auditService;

    @Override
    public DeliveryRole createRole(String roleName, String roleDescription, UserDTO user) {

        roleRepo.findByRoleNameIgnoreCase(roleName)
                .ifPresent(r -> {
                    throw new IllegalStateException("Duplicate delivery role");
                });

        DeliveryRole role = DeliveryRole.builder()
                .roleName(roleName)
                .roleDescription(roleDescription)
                .activeFlag(true)
                .build();

        DeliveryRole saved = roleRepo.save(role);

        auditService.auditCreate(
                "DELIVERY_ROLE",
                saved.getRoleId().toString(),
                saved,
                user.getEmail()
        );

        return saved;
    }

    @Override
    public void deactivateRole(UUID roleId, UserDTO user) {
        DeliveryRole role = roleRepo.findById(roleId)
                .orElseThrow(() -> new IllegalStateException("Role not found"));

        if (!role.getActiveFlag()) return;

        DeliveryRole before = DeliveryRole.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .activeFlag(role.getActiveFlag())
                .build();

        role.setActiveFlag(false);
        roleRepo.save(role);

        auditService.auditUpdate(
                "DELIVERY_ROLE",
                role.getRoleId().toString(),
                before,
                role,
                user.getEmail()
        );
    }

    @Override
    public Optional<DeliveryRole> getActiveRole(UUID roleId) {
        return roleRepo.findById(roleId).filter(DeliveryRole::getActiveFlag);
    }

    @Override
    public List<DeliveryRole> getAllActiveRoles() {
        return roleRepo.findAll()
                .stream()
                .filter(DeliveryRole::getActiveFlag)
                .toList();
    }
}
