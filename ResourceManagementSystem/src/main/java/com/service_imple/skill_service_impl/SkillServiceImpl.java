package com.service_imple.skill_service_impl;

import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SkillCategory;
import com.repo.skill_repo.SkillCategoryRepository;
import com.repo.skill_repo.SkillRepository;
import com.service_interface.skill_service_interface.SkillAuditService;
import com.service_interface.skill_service_interface.SkillService;
import com.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepo;
    private final SkillCategoryRepository categoryRepo;
    private final SkillAuditService auditService;

    @Override
    public Skill createSkill(Long categoryId, String skillName, String description,
                             boolean isCertification, String proficiencyName, UserDTO user) {

        SkillCategory category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new IllegalStateException("Invalid category"));

        skillRepo.findBySkillNameIgnoreCase(skillName)
                .ifPresent(s -> {
                    throw new IllegalStateException("Duplicate skill name");
                });

        Skill skill = Skill.builder()
                .category(category)
                .skillName(skillName)
                .skillDescription(description)
                .isCertification(isCertification)
                .activeFlag(true)
                .build();

        Skill saved = skillRepo.save(skill);

        auditService.auditCreate(
                "SKILL",
                saved.getSkillId().toString(),
                saved,
                user.getEmail()
        );

        return saved;
    }

    @Override
    public Skill updateSkill(Long skillId, String skillName, String description,
                             String proficiencyName, UserDTO user) {

        Skill existing = skillRepo.findById(skillId)
                .orElseThrow(() -> new IllegalStateException("Skill not found"));

        Skill before = Skill.builder()
                .skillId(existing.getSkillId())
                .skillName(existing.getSkillName())
                .skillDescription(existing.getSkillDescription())
                .isCertification(existing.getIsCertification())
                .activeFlag(existing.getActiveFlag())
                .build();

        existing.setSkillName(skillName);
        existing.setSkillDescription(description);

        Skill saved = skillRepo.save(existing);

        auditService.auditUpdate(
                "SKILL",
                saved.getSkillId().toString(),
                before,
                saved,
                user.getEmail()
        );

        return saved;
    }

    @Override
    public void deactivateSkill(Long skillId, UserDTO user) {
        Skill existing = skillRepo.findById(skillId)
                .orElseThrow(() -> new IllegalStateException("Skill not found"));

        if (!existing.getActiveFlag()) return;

        Skill before = Skill.builder()
                .skillId(existing.getSkillId())
                .skillName(existing.getSkillName())
                .activeFlag(existing.getActiveFlag())
                .build();

        existing.setActiveFlag(false);
        skillRepo.save(existing);

        auditService.auditUpdate(
                "SKILL",
                existing.getSkillId().toString(),
                before,
                existing,
                user.getEmail()
        );
    }

    @Override
    public Optional<Skill> getActiveSkill(Long skillId) {
        return skillRepo.findById(skillId).filter(Skill::getActiveFlag);
    }

    @Override
    public List<Skill> getActiveSkillsByCategory(Long categoryId) {
        return skillRepo.findByCategoryCategoryIdAndActiveFlagTrue(categoryId);
    }
}
