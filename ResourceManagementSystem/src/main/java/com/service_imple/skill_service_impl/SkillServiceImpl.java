package com.service_imple.skill_service_impl;

import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SkillCategory;
import com.entity.skill_entities.SubSkill;
import com.global_exception_handler.SkillExceptionHandler;
import com.repo.skill_repo.*;
import com.service_interface.skill_service_interface.SkillService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;
    private final SkillCategoryRepository categoryRepository;
    private final ResourceSkillRepository resourceSkillRepository;
    private final SubSkillRepository subSkillRepository;
    private final ResourceSubSkillRepository resourceSubSkillRepository;

    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }

    @Override
    public Skill create(UUID categoryId, String name, String description) {

        SkillCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> SkillExceptionHandler.badRequest("Category not found"));

        String normalized = normalize(name);

        boolean exists =
                skillRepository.findAll()
                        .stream()
                        .anyMatch(skill ->

                                normalize(skill.getName())
                                        .equals(normalized)

                                        &&

                                        skill.getCategory()
                                                .getId()
                                                .equals(categoryId)
                        );

        if (exists) {
            throw SkillExceptionHandler.badRequest("Skill already exists in this category");
        }

        Skill skill = new Skill();
        skill.setName(normalized);
        skill.setDescription(description);
        skill.setCategory(category);

        return skillRepository.save(skill);
    }

    @Transactional
    @Override
    public Skill delete(UUID skillId) {

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() ->
                        SkillExceptionHandler.notFound(
                                "Skill not found"));

        // ==========================================
        // CHECK DIRECT SKILL ASSIGNMENT
        // ==========================================

        boolean skillAssigned =
                resourceSkillRepository
                        .existsBySkillIdAndActiveFlagTrue(skillId);

        if (skillAssigned) {

            throw SkillExceptionHandler.badRequest(
                    "Cannot delete skill. Resources are assigned to this skill.");
        }

        // ==========================================
        // CHECK SUBSKILL ASSIGNMENT
        // ==========================================

        List<SubSkill> subSkills =
                subSkillRepository.findBySkillId(skillId);

        for (SubSkill subSkill : subSkills) {

            boolean subSkillAssigned =
                    resourceSubSkillRepository
                            .existsBySubSkillId(subSkill.getId());

            if (subSkillAssigned) {

                throw SkillExceptionHandler.badRequest(
                        "Cannot delete skill. Resources are assigned to subskills under this skill.");
            }
        }

        // ==========================================
        // SOFT DELETE (RECOMMENDED)
        // ==========================================

        skill.setStatus("INACTIVE");

        skillRepository.save(skill);

        return skill;
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
                .orElseThrow(() -> SkillExceptionHandler.badRequest("Skill not found"));

        if (!"ACTIVE".equals(skill.getStatus())) {
            throw SkillExceptionHandler.badRequest("Skill is already inactive");
        }

        long activeSubSkillsCount = skillRepository.countActiveSubSkillsBySkillId(skillId);
        if (activeSubSkillsCount > 0) {
            throw SkillExceptionHandler.badRequest("Cannot deactivate skill with " + activeSubSkillsCount + " active sub-skills");
        }

        int updated = skillRepository.deactivateSkill(skillId);
        if (updated == 0) {
            throw SkillExceptionHandler.badRequest("Failed to deactivate skill");
        }
    }

    @Override
    public Skill update(UUID skillId, UUID categoryId, String name, String description) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> SkillExceptionHandler.badRequest("Skill not found"));

        SkillCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> SkillExceptionHandler.badRequest("Category not found"));

        String normalized = normalize(name);

        boolean exists =
                skillRepository.findAll()
                        .stream()
                        .anyMatch(existing ->

                                !existing.getId().equals(skillId)

                                        &&

                                        normalize(existing.getName())
                                                .equals(normalized)

                                        &&

                                        existing.getCategory()
                                                .getId()
                                                .equals(categoryId)
                        );

        if (exists) {

            throw SkillExceptionHandler.badRequest(
                    "Skill already exists in this category");
        }

        skill.setName(normalized);
        skill.setDescription(description);
        skill.setCategory(category);

        return skillRepository.save(skill);
    }
}
