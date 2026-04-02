package com.service_imple.skill_service_impl;

import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SkillCategory;
import com.global_exception_handler.SkillTaxonomyExceptionHandler;
import com.repo.skill_repo.SkillCategoryRepository;
import com.repo.skill_repo.SkillRepository;
import com.service_interface.skill_service_interface.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;
    private final SkillCategoryRepository categoryRepository;

    @Override
    public Skill create(UUID categoryId, String name, String description) {

        SkillCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler("Category not found"));

        String normalized = name.trim();

        if (skillRepository.existsByNameIgnoreCaseAndCategory_Id(normalized, categoryId)) {
            throw new SkillTaxonomyExceptionHandler("Skill already exists in this category");
        }

        Skill skill = new Skill();
        skill.setName(normalized);
        skill.setDescription(description);
        skill.setCategory(category);

        return skillRepository.save(skill);
    }

    @Override
    public List<Skill> findActiveSkills() {
        return skillRepository.findActiveSkills();
    }

    @Override
    public List<Skill> findActiveSkillsByCategoryId(UUID categoryId) {
        return skillRepository.findActiveSkillsByCategoryId(categoryId);
    }

    @Override
    public void deactivateSkill(UUID skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler("Skill not found"));

        if (!"ACTIVE".equals(skill.getStatus())) {
            throw new SkillTaxonomyExceptionHandler("Skill is already inactive");
        }

        long activeSubSkillsCount = skillRepository.countActiveSubSkillsBySkillId(skillId);
        if (activeSubSkillsCount > 0) {
            throw new SkillTaxonomyExceptionHandler("Cannot deactivate skill with " + activeSubSkillsCount + " active sub-skills");
        }

        int updated = skillRepository.deactivateSkill(skillId);
        if (updated == 0) {
            throw new SkillTaxonomyExceptionHandler("Failed to deactivate skill");
        }
    }

    @Override
    public Skill update(UUID skillId, UUID categoryId, String name, String description) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler("Skill not found"));

        SkillCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler("Category not found"));

        String normalized = name.trim();

        if (!skill.getCategory().getId().equals(categoryId) && 
            skillRepository.existsByNameIgnoreCaseAndCategory_Id(normalized, categoryId)) {
            throw new SkillTaxonomyExceptionHandler("Skill already exists in this category");
        }

        if (!skill.getName().equalsIgnoreCase(normalized) || !skill.getCategory().getId().equals(categoryId)) {
            if (skillRepository.existsByNameIgnoreCaseAndCategory_IdAndIdNot(normalized, categoryId, skillId)) {
                throw new SkillTaxonomyExceptionHandler("Skill already exists in this category");
            }
        }

        skill.setName(normalized);
        skill.setDescription(description);
        skill.setCategory(category);

        return skillRepository.save(skill);
    }
}
