package com.service_imple.skill_service_impl;

import com.dto.skill_dto.*;
import com.entity.skill_entities.*;
import com.global_exception_handler.SkillExceptionHandler;
import com.repo.skill_repo.*;
import com.service_interface.skill_service_interface.EmployeeSkillMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeSkillMappingServiceImpl implements EmployeeSkillMappingService {

    private final EmployeeSkillMappingRepository employeeSkillMappingRepository;
    private final SkillCategoryRepository skillCategoryRepository;
    private final SkillRepository skillRepository;
    private final SubSkillRepository subSkillRepository;
    private final ProficiencyLevelRepository proficiencyLevelRepository;

    // -------------------------------------------------------------------------
    // Bulk save/upsert — used by POST /api/employee-skills
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public String saveSkillMappings(EmployeeSkillsRequestDto requestDto) {
        if (requestDto.getResourceId() == null || requestDto.getResourceId().isBlank()) {
            throw SkillExceptionHandler.badRequest("Resource ID is required");
        }
        if (requestDto.getSkills() == null || requestDto.getSkills().isEmpty()) {
            throw SkillExceptionHandler.badRequest("At least one skill is required");
        }

        for (SkillMappingItemDto skillDto : requestDto.getSkills()) {
            if (skillDto.getSkillId() == null) {
                throw SkillExceptionHandler.badRequest("skillId is required for every skill entry");
            }

            Skill skill = skillRepository.findById(skillDto.getSkillId())
                    .orElseThrow(() -> SkillExceptionHandler.notFound(
                            "Skill not found: " + skillDto.getSkillId()));

            SkillCategory category = skill.getCategory();
            if (category == null) {
                throw SkillExceptionHandler.badRequest(
                        "Skill has no associated category: " + skillDto.getSkillId());
            }

            ProficiencyLevel skillProficiency = resolveProficiency(skillDto.getProficiencyId());
            String skillStatus = resolveStatus(skillDto.getStatus());

            if (skillDto.getSubSkills() == null || skillDto.getSubSkills().isEmpty()) {
                upsertMapping(requestDto.getResourceId(), category, skill, null,
                        skillProficiency, skillStatus);
            } else {
                for (SubSkillMappingItemDto subSkillDto : skillDto.getSubSkills()) {
                    if (subSkillDto.getSubSkillId() == null) {
                        throw SkillExceptionHandler.badRequest(
                                "subSkillId is required for every subSkill entry");
                    }

                    SubSkill subSkill = subSkillRepository.findById(subSkillDto.getSubSkillId())
                            .orElseThrow(() -> SkillExceptionHandler.notFound(
                                    "SubSkill not found: " + subSkillDto.getSubSkillId()));

                    // SubSkill proficiency overrides skill-level; fall back to skill proficiency
                    ProficiencyLevel effectiveProficiency = subSkillDto.getProficiencyId() != null
                            ? resolveProficiency(subSkillDto.getProficiencyId())
                            : skillProficiency;

                    String effectiveStatus = resolveStatus(subSkillDto.getStatus() != null
                            ? subSkillDto.getStatus() : skillDto.getStatus());

                    upsertMapping(requestDto.getResourceId(), category, skill, subSkill,
                            effectiveProficiency, effectiveStatus);
                }
            }
        }

        return "Skills mapped successfully";
    }

    // -------------------------------------------------------------------------
    // Single-skill save — used internally by the skill request approval flow
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public String saveSkillMapping(EmployeeSkillRequestDto requestDto) {
        if (requestDto.getEmployeeId() == null || requestDto.getEmployeeId().isBlank()) {
            throw SkillExceptionHandler.badRequest("Employee ID is required");
        }
        if (requestDto.getCategoryId() == null) {
            throw SkillExceptionHandler.badRequest("Category ID is required");
        }
        if (requestDto.getSkillId() == null) {
            throw SkillExceptionHandler.badRequest("Skill ID is required");
        }

        SkillCategory category = skillCategoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> SkillExceptionHandler.notFound("Category not found"));

        Skill skill = skillRepository.findById(requestDto.getSkillId())
                .orElseThrow(() -> SkillExceptionHandler.notFound("Skill not found"));

        SubSkill subSkill = null;
        if (requestDto.getSubskillId() != null) {
            subSkill = subSkillRepository.findById(requestDto.getSubskillId())
                    .orElseThrow(() -> SkillExceptionHandler.notFound("SubSkill not found"));
        }

        boolean duplicate = employeeSkillMappingRepository
                .existsByEmployeeIdAndSkill_IdAndSubSkill_Id(
                        requestDto.getEmployeeId(),
                        requestDto.getSkillId(),
                        requestDto.getSubskillId());
        if (duplicate) {
            throw SkillExceptionHandler.conflict("Skill already mapped to this employee");
        }

        ProficiencyLevel proficiency = resolveProficiency(requestDto.getProficiencyId());

        EmployeeSkillMapping mapping = new EmployeeSkillMapping();
        mapping.setEmployeeId(requestDto.getEmployeeId());
        mapping.setCategory(category);
        mapping.setSkill(skill);
        mapping.setSubSkill(subSkill);
        mapping.setProficiency(proficiency);
        mapping.setStatus(resolveStatus(requestDto.getStatus()));

        employeeSkillMappingRepository.save(mapping);
        return "Skill mapped successfully";
    }

    // -------------------------------------------------------------------------
    // Fetch
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSkillResponseDto> getEmployeeSkills(String resourceId) {
        return employeeSkillMappingRepository.findActiveSkillsByEmployeeId(resourceId)
                .stream()
                .map(m -> EmployeeSkillResponseDto.builder()
                        .id(m.getId())
                        .employeeId(m.getEmployeeId())
                        .categoryName(m.getCategory() != null ? m.getCategory().getName() : null)
                        .skillName(m.getSkill() != null ? m.getSkill().getName() : null)
                        .subskillName(m.getSubSkill() != null ? m.getSubSkill().getName() : null)
                        .proficiency(m.getProficiency() != null
                                ? m.getProficiency().getProficiencyName() : null)
                        .status(m.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void upsertMapping(String resourceId, SkillCategory category, Skill skill,
                               SubSkill subSkill, ProficiencyLevel proficiency, String status) {
        UUID subSkillId = subSkill != null ? subSkill.getId() : null;

        employeeSkillMappingRepository
                .findExistingMapping(resourceId, skill.getId(), subSkillId)
                .ifPresentOrElse(existing -> {
                    existing.setProficiency(proficiency);
                    existing.setStatus(status);
                    employeeSkillMappingRepository.save(existing);
                }, () -> {
                    EmployeeSkillMapping mapping = new EmployeeSkillMapping();
                    mapping.setEmployeeId(resourceId);
                    mapping.setCategory(category);
                    mapping.setSkill(skill);
                    mapping.setSubSkill(subSkill);
                    mapping.setProficiency(proficiency);
                    mapping.setStatus(status);
                    employeeSkillMappingRepository.save(mapping);
                });
    }

    private ProficiencyLevel resolveProficiency(UUID proficiencyId) {
        if (proficiencyId == null) return null;
        return proficiencyLevelRepository.findById(proficiencyId)
                .orElseThrow(() -> SkillExceptionHandler.notFound(
                        "Proficiency level not found: " + proficiencyId));
    }

    private String resolveStatus(String status) {
        return (status != null && !status.isBlank()) ? status : "ACTIVE";
    }
}
