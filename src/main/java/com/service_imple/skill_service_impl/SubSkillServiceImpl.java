package com.service_imple.skill_service_impl;

import com.dto.skill_dto.SubSkillItemDTO;
import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SubSkill;
import com.global_exception_handler.SkillExceptionHandler;
import com.repo.skill_repo.ResourceSkillRepository;
import com.repo.skill_repo.ResourceSubSkillRepository;
import com.repo.skill_repo.SkillRepository;
import com.repo.skill_repo.SubSkillRepository;
import com.service_interface.skill_service_interface.SubSkillService;
import jakarta.transaction.Transactional;
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
    public SubSkill create(UUID skillId, String name, String description) {

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> SkillExceptionHandler.badRequest("Parent skill not found"));

        String normalized = normalize(name);

        boolean exists =
                subSkillRepository.findAll()
                        .stream()
                        .anyMatch(subSkill ->

                                normalize(subSkill.getName())
                                        .equals(normalized)

                                        &&

                                        subSkill.getSkill()
                                                .getId()
                                                .equals(skillId)
                        );

        if (exists) {
            throw SkillExceptionHandler.badRequest("Sub-skill already exists under this skill");
        }

        SubSkill subSkill = new SubSkill();
        subSkill.setName(normalized);
        subSkill.setDescription(description);
        subSkill.setSkill(skill);
        String normalizedSkill =
                normalize(skill.getName());

        boolean duplicateCombination =
                subSkillRepository.findAll()
                        .stream()
                        .anyMatch(existing ->

                                normalize(existing.getSkill().getName())
                                        .equals(normalizedSkill)

                                        &&

                                        normalize(existing.getName())
                                                .equals(normalized)
                        );

        if (duplicateCombination) {

            throw SkillExceptionHandler.badRequest(
                    "This Skill + SubSkill combination already exists.");
        }

        SubSkill saved = subSkillRepository.save(subSkill);
        skill.addSubSkill(saved);
        
        return saved;
    }

    @Transactional
    @Override
    public void deleteSubSkill(UUID subSkillId) {

        SubSkill subSkill =
                subSkillRepository.findById(subSkillId)
                        .orElseThrow(() ->
                                SkillExceptionHandler.notFound(
                                        "SubSkill not found"));

        boolean assigned =
                resourceSubSkillRepository
                        .existsBySubSkillId(subSkillId);

        if (assigned) {

            throw SkillExceptionHandler.badRequest(
                    "Cannot delete subskill. Resources are assigned to this subskill.");
        }

        subSkillRepository.delete(subSkill);
    }

    @Override
    public List<SubSkill> createMultiple(UUID skillId, List<SubSkillItemDTO> subSkillItems) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> SkillExceptionHandler.badRequest("Parent skill not found"));

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
            throw SkillExceptionHandler.badRequest("Some sub-skills already exist: " + String.join(", ", existingNames));
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
                .orElseThrow(() -> SkillExceptionHandler.badRequest("Sub-skill not found"));

        if (!"ACTIVE".equals(subSkill.getStatus())) {
            throw SkillExceptionHandler.badRequest("Sub-skill is already inactive");
        }

        int updated = subSkillRepository.deactivateSubSkill(subSkillId);
        if (updated == 0) {
            throw SkillExceptionHandler.badRequest("Failed to deactivate sub-skill");
        }
    }

    @Override
    public SubSkill update(UUID subSkillId, UUID skillId, String name, String description) {
        SubSkill subSkill = subSkillRepository.findById(subSkillId)
                .orElseThrow(() -> SkillExceptionHandler.badRequest("Sub-skill not found"));

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> SkillExceptionHandler.badRequest("Parent skill not found"));

        String normalized = normalize(name);

        boolean exists =
                subSkillRepository.findAll()
                        .stream()
                        .anyMatch(existing ->

                                !existing.getId()
                                        .equals(subSkillId)

                                        &&

                                        normalize(existing.getName())
                                                .equals(normalized)

                                        &&

                                        existing.getSkill()
                                                .getId()
                                                .equals(skillId)
                        );

        if (exists) {

            throw SkillExceptionHandler.badRequest(
                    "Sub-skill already exists under this skill");
        }
        String normalizedSkill =
                normalize(skill.getName());

        boolean duplicateCombination =
                subSkillRepository.findAll()
                        .stream()
                        .anyMatch(existing ->

                                !existing.getId()
                                        .equals(subSkillId)

                                        &&

                                        normalize(existing.getSkill().getName())
                                                .equals(normalizedSkill)

                                        &&

                                        normalize(existing.getName())
                                                .equals(normalized)
                        );

        if (duplicateCombination) {

            throw SkillExceptionHandler.badRequest(
                    "This Skill + SubSkill combination already exists.");
        }

        subSkill.setName(normalized);
        subSkill.setDescription(description);
        subSkill.setSkill(skill);

        return subSkillRepository.save(subSkill);
    }
}
