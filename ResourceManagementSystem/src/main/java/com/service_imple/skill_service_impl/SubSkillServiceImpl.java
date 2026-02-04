package com.service_imple.skill_service_impl;

import com.dto.UserDTO;
import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SubSkill;
import com.repo.skill_repo.SkillRepository;
import com.repo.skill_repo.SubSkillRepository;
import com.service_interface.skill_service_interface.SkillAuditService;
import com.service_interface.skill_service_interface.SubSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubSkillServiceImpl implements SubSkillService {

    private final SkillRepository skillRepo;
    private final SubSkillRepository subSkillRepo;
    private final SkillAuditService auditService;

    @Override
    public SubSkill createSubSkill(UUID skillId, String subSkillName,
                                   String description, boolean isCertification,
                                   String proficiencyName, UserDTO user) {

        Skill parentSkill = skillRepo.findById(skillId)
                .orElseThrow(() -> new IllegalStateException("Parent skill not found"));

        SubSkill subSkill = SubSkill.builder()
                .skill(parentSkill)
                .subSkillName(subSkillName)
                .subSkillDescription(description)
                .isCertification(isCertification)
                .activeFlag(true)
                .build();

        SubSkill saved = subSkillRepo.save(subSkill);

        auditService.auditCreate(
                "SUB_SKILL",
                saved.getSubSkillId().toString(),
                saved,
                user.getEmail()
        );

        return saved;
    }

    @Override
    public void deactivateSubSkill(UUID subSkillId, UserDTO user) {
        SubSkill existing = subSkillRepo.findById(subSkillId)
                .orElseThrow(() -> new IllegalStateException("Sub-skill not found"));

        if (!existing.getActiveFlag()) return;

        SubSkill before = SubSkill.builder()
                .subSkillId(existing.getSubSkillId())
                .subSkillName(existing.getSubSkillName())
                .activeFlag(existing.getActiveFlag())
                .build();

        existing.setActiveFlag(false);
        subSkillRepo.save(existing);

        auditService.auditUpdate(
                "SUB_SKILL",
                existing.getSubSkillId().toString(),
                before,
                existing,
                user.getEmail()
        );
    }

    @Override
    public List<SubSkill> getActiveSubSkills(UUID skillId) {
        return subSkillRepo.findBySkillSkillIdAndActiveFlagTrue(skillId);
    }
}
