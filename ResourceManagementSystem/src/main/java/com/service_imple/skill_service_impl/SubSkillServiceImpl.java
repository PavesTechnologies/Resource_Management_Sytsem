package com.service_imple.skill_service_impl;

import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SubSkill;
import com.global_exception_handler.SkillTaxonomyExceptionHandler;
import com.repo.skill_repo.SkillRepository;
import com.repo.skill_repo.SubSkillRepository;
import com.service_interface.skill_service_interface.SubSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubSkillServiceImpl implements SubSkillService {

    private final SubSkillRepository subSkillRepository;
    private final SkillRepository skillRepository;

    @Override
    public SubSkill create(UUID skillId, String name, String description) {

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler("Parent skill not found"));

        String normalized = name.trim();

        if (subSkillRepository.existsByNameIgnoreCaseAndSkill_Id(normalized, skillId)) {
            throw new SkillTaxonomyExceptionHandler("Sub-skill already exists under this skill");
        }

        SubSkill subSkill = new SubSkill();
        subSkill.setName(normalized);
        subSkill.setDescription(description);
        subSkill.setSkill(skill);

        return subSkillRepository.save(subSkill);
    }

    @Override
    public List<SubSkill> findActiveSubSkills() {
        return subSkillRepository.findActiveSubSkills();
    }

    @Override
    public List<SubSkill> findActiveSubSkillsBySkillId(UUID skillId) {
        return subSkillRepository.findActiveSubSkillsBySkillId(skillId);
    }

    @Override
    public void deactivateSubSkill(UUID subSkillId) {
        SubSkill subSkill = subSkillRepository.findById(subSkillId)
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler("Sub-skill not found"));

        if (!"ACTIVE".equals(subSkill.getStatus())) {
            throw new SkillTaxonomyExceptionHandler("Sub-skill is already inactive");
        }

        int updated = subSkillRepository.deactivateSubSkill(subSkillId);
        if (updated == 0) {
            throw new SkillTaxonomyExceptionHandler("Failed to deactivate sub-skill");
        }
    }
}
