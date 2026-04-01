package com.service_imple.skill_service_impl;

import com.dto.skill_dto.SubSkillItemDTO;
import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SubSkill;
import com.global_exception_handler.SkillTaxonomyExceptionHandler;
import com.repo.skill_repo.SkillRepository;
import com.repo.skill_repo.SubSkillRepository;
import com.service_interface.skill_service_interface.SubSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

        SubSkill saved = subSkillRepository.save(subSkill);
        skill.addSubSkill(saved);
        
        return saved;
    }

    @Override
    public List<SubSkill> createMultiple(UUID skillId, List<SubSkillItemDTO> subSkillItems) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler("Parent skill not found"));

        List<SubSkill> createdSubSkills = new ArrayList<>();
        List<String> existingNames = new ArrayList<>();

        for (SubSkillItemDTO item : subSkillItems) {
            String normalized = item.getName().trim();
            
            if (subSkillRepository.existsByNameIgnoreCaseAndSkill_Id(normalized, skillId)) {
                existingNames.add(normalized);
                continue;
            }

            SubSkill subSkill = new SubSkill();
            subSkill.setName(normalized);
            subSkill.setDescription(item.getDescription());
            subSkill.setSkill(skill);
            
            // Set status based on isActive flag
            if (item.getIsActive() != null && !item.getIsActive()) {
                subSkill.setStatus("INACTIVE");
            } else {
                subSkill.setStatus("ACTIVE");
            }

            SubSkill saved = subSkillRepository.save(subSkill);
            skill.addSubSkill(saved);
            createdSubSkills.add(saved);
        }

        if (!existingNames.isEmpty()) {
            throw new SkillTaxonomyExceptionHandler("Some sub-skills already exist: " + String.join(", ", existingNames));
        }

        return createdSubSkills;
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

    @Override
    public SubSkill update(UUID subSkillId, UUID skillId, String name, String description) {
        SubSkill subSkill = subSkillRepository.findById(subSkillId)
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler("Sub-skill not found"));

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler("Parent skill not found"));

        String normalized = name.trim();

        if (!subSkill.getSkill().getId().equals(skillId) && 
            subSkillRepository.existsByNameIgnoreCaseAndSkill_Id(normalized, skillId)) {
            throw new SkillTaxonomyExceptionHandler("Sub-skill already exists under this skill");
        }

        if (!subSkill.getName().equalsIgnoreCase(normalized) || !subSkill.getSkill().getId().equals(skillId)) {
            if (subSkillRepository.existsByNameIgnoreCaseAndSkill_IdAndIdNot(normalized, skillId, subSkillId)) {
                throw new SkillTaxonomyExceptionHandler("Sub-skill already exists under this skill");
            }
        }

        subSkill.setName(normalized);
        subSkill.setDescription(description);
        subSkill.setSkill(skill);

        return subSkillRepository.save(subSkill);
    }
}
