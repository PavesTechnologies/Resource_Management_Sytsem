package com.service_imple.skill_service_impl;

import com.dto.UserDTO;
import com.entity.skill_entities.ResourceSkill;
import com.repo.skill_repo.ResourceSkillRepository;
import com.service_interface.skill_service_interface.SkillAuditService;
import com.service_interface.skill_service_interface.ResourceSkillService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceSkillServiceImpl implements ResourceSkillService {

    private final ResourceSkillRepository resourceSkillRepo;
    private final SkillAuditService auditService;

    @Override
    public ResourceSkill addOrUpdateResourceSkill(
            Long resourceId,
            UUID skillId,
            LocalDate lastUsedDate,
            LocalDate expiryDate,
            UserDTO user) {

        ResourceSkill existing = resourceSkillRepo
                .findByResourceIdAndSkillId(resourceId, skillId)
                .orElse(null);

        if (existing == null) {
            ResourceSkill rs = ResourceSkill.builder()
                    .resourceId(resourceId)
                    .skillId(skillId)
                    .lastUsedDate(lastUsedDate)
                    .expiryDate(expiryDate)
                    .activeFlag(true)
                    .build();

            ResourceSkill saved = resourceSkillRepo.save(rs);

            auditService.auditCreate(
                    "RESOURCE_SKILL",
                    saved.getId().toString(),
                    saved,
                    user.getEmail()
            );

            return saved;
        }

        ResourceSkill before = ResourceSkill.builder()
                .id(existing.getId())
                .resourceId(existing.getResourceId())
                .skillId(existing.getSkillId())
                .lastUsedDate(existing.getLastUsedDate())
                .expiryDate(existing.getExpiryDate())
                .activeFlag(existing.getActiveFlag())
                .build();

        existing.setLastUsedDate(lastUsedDate);
        existing.setExpiryDate(expiryDate);

        ResourceSkill saved = resourceSkillRepo.save(existing);

        auditService.auditUpdate(
                "RESOURCE_SKILL",
                saved.getId().toString(),
                before,
                saved,
                user.getEmail()
        );

        return saved;
    }

    @Override
    public List<ResourceSkill> getActiveSkillsForResource(Long resourceId) {
        return resourceSkillRepo.findByResourceIdAndActiveFlagTrue(resourceId);
    }
}
