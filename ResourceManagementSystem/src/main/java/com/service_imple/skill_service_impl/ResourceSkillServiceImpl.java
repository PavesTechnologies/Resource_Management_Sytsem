package com.service_imple.skill_service_impl;

import com.global_exception_handler.SkillTaxonomyExceptionHandler;
import com.service_interface.skill_service_interface.ResourceSkillService;
import com.dto.skill_dto.ResourceSkillBulkRequestDTO;
import com.dto.skill_dto.ResourceSkillProfileDTO;
import com.dto.skill_dto.SkillWithSubSkillDTO;
import com.dto.skill_dto.SubSkillDTO;
import com.entity.skill_entities.ProficiencyLevel;
import com.entity.skill_entities.ResourceSkill;
import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SubSkill;
import com.repo.skill_repo.ProficiencyLevelRepository;
import com.repo.skill_repo.ResourceSkillRepository;
import com.repo.skill_repo.SkillRepository;
import com.repo.skill_repo.SubSkillRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceSkillServiceImpl implements ResourceSkillService {
    private final ResourceSkillRepository resourceSkillRepository;
    private final SkillRepository skillRepository;
    private final SubSkillRepository subSkillRepository;
    private final ProficiencyLevelRepository proficiencyLevelRepository;

    @Override
    @Transactional
    public String addSkillsToResource(ResourceSkillBulkRequestDTO dto) {
        // Validate all skills before saving (atomic validation)
        validateAllSkills(dto);
        
        // Save all validated skills
        List<ResourceSkill> resourceSkills = new ArrayList<>();
        
        for (SkillWithSubSkillDTO skillDTO : dto.getSkills()) {
            // Add skill-level proficiency
            ResourceSkill skillResource = ResourceSkill.builder()
                    .resourceId(dto.getResourceId())
                    .skillId(skillDTO.getSkillId())
                    .subSkillId(null) // null indicates skill-level proficiency
                    .proficiencyId(skillDTO.getProficiencyId())
                    .lastUsedDate(LocalDate.now())
                    .expiryDate(null)
                    .activeFlag(true)
                    .build();
            resourceSkills.add(skillResource);
            
            // Add sub-skill proficiencies if any
            if (skillDTO.getSubSkills() != null) {
                for (SubSkillDTO subSkillDTO : skillDTO.getSubSkills()) {
                    ResourceSkill subSkillResource = ResourceSkill.builder()
                            .resourceId(dto.getResourceId())
                            .skillId(skillDTO.getSkillId())
                            .subSkillId(subSkillDTO.getSubSkillId())
                            .proficiencyId(subSkillDTO.getProficiencyId())
                            .lastUsedDate(LocalDate.now())
                            .expiryDate(null)
                            .activeFlag(true)
                            .build();
                    resourceSkills.add(subSkillResource);
                }
            }
        }
        
        resourceSkillRepository.saveAll(resourceSkills);
        return "Skills successfully added";
    }
    
    private void validateAllSkills(ResourceSkillBulkRequestDTO dto) {
        for (SkillWithSubSkillDTO skillDTO : dto.getSkills()) {
            // Validate skill exists and is ACTIVE
            Skill skill = skillRepository.findById(skillDTO.getSkillId())
                    .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                            "Skill not found: " + skillDTO.getSkillId()));
            
            if (!"ACTIVE".equalsIgnoreCase(skill.getStatus())) {
                throw new SkillTaxonomyExceptionHandler(
                        "Skill is not active: " + skill.getName());
            }
            
            // Validate skill proficiency exists and is ACTIVE
            ProficiencyLevel skillProficiency = proficiencyLevelRepository
                    .findById(skillDTO.getProficiencyId())
                    .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                            "Proficiency not found: " + skillDTO.getProficiencyId()));
            
            if (!Boolean.TRUE.equals(skillProficiency.getActiveFlag())) {
                throw new SkillTaxonomyExceptionHandler(
                        "Proficiency level is inactive: " + skillProficiency.getProficiencyName());
            }
            
            // Prevent duplicate skill-level proficiency
            boolean skillExists = resourceSkillRepository
                    .existsByResourceIdAndSkillIdAndActiveFlagTrue(
                            dto.getResourceId(),
                            skillDTO.getSkillId());
            
            if (skillExists) {
                throw new SkillTaxonomyExceptionHandler(
                        "Skill already assigned to this resource: " + skill.getName());
            }
            
            // Validate sub-skills if any
            if (skillDTO.getSubSkills() != null) {
                for (SubSkillDTO subSkillDTO : skillDTO.getSubSkills()) {
                    validateSubSkill(dto.getResourceId(), skillDTO.getSkillId(), 
                            subSkillDTO, skillProficiency);
                }
            }
        }
    }
    
    private void validateSubSkill(UUID resourceId, UUID skillId, SubSkillDTO subSkillDTO, 
                                 ProficiencyLevel skillProficiency) {
        // Validate subSkill exists
        SubSkill subSkill = subSkillRepository.findById(subSkillDTO.getSubSkillId())
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                        "SubSkill not found: " + subSkillDTO.getSubSkillId()));
        
        // Validate subSkill belongs to Skill
        if (!skillId.equals(subSkill.getSkill().getId())) {
            throw new SkillTaxonomyExceptionHandler(
                    "SubSkill does not belong to the specified skill: " + subSkill.getName());
        }
        
        // Validate subSkill is ACTIVE
        if (!"ACTIVE".equalsIgnoreCase(subSkill.getStatus())) {
            throw new SkillTaxonomyExceptionHandler(
                    "SubSkill is not active: " + subSkill.getName());
        }
        
        // Validate subSkill proficiency exists and is ACTIVE
        ProficiencyLevel subSkillProficiency = proficiencyLevelRepository
                .findById(subSkillDTO.getProficiencyId())
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                        "Proficiency not found for subSkill: " + subSkillDTO.getProficiencyId()));
        
        if (!Boolean.TRUE.equals(subSkillProficiency.getActiveFlag())) {
            throw new SkillTaxonomyExceptionHandler(
                    "Proficiency level is inactive for subSkill: " + subSkillProficiency.getProficiencyName());
        }
        
        // Validate subSkill proficiency <= skill proficiency (using display order)
        if (subSkillProficiency.getDisplayOrder() > skillProficiency.getDisplayOrder()) {
            throw new SkillTaxonomyExceptionHandler(
                    "SubSkill proficiency cannot exceed skill proficiency for: " + subSkill.getName());
        }
        
        // Prevent duplicate sub-skill proficiency
        boolean subSkillExists = resourceSkillRepository
                .existsByResourceIdAndSkillIdAndSubSkillIdAndActiveFlagTrue(
                        resourceId,
                        skillId,
                        subSkillDTO.getSubSkillId());
        
        if (subSkillExists) {
            throw new SkillTaxonomyExceptionHandler(
                    "SubSkill already assigned to this resource: " + subSkill.getName());
        }
    }

    @Override
    public List<ResourceSkillProfileDTO> getResourceSkillProfile(UUID resourceId) {
        List<ResourceSkill> skills = resourceSkillRepository.findByResourceIdAndActiveFlagTrue(resourceId);
        
        return skills.stream().map(rs -> {
            Skill skill = skillRepository.findById(rs.getSkillId()).orElseThrow();
            ProficiencyLevel proficiency = proficiencyLevelRepository.findById(rs.getProficiencyId()).orElseThrow();
            
            String skillName = skill.getName();
            
            // If subSkillId is not null, it's a sub-skill proficiency
            if (rs.getSubSkillId() != null) {
                SubSkill subSkill = subSkillRepository.findById(rs.getSubSkillId()).orElseThrow();
                skillName = skill.getName() + " - " + subSkill.getName();
            }
            
            return ResourceSkillProfileDTO.builder()
                    .category(skill.getCategory().getName())
                    .skill(skillName)
                    .proficiency(proficiency.getProficiencyName())
                    .proficiencyCode(proficiency.getProficiencyCode())
                    .lastUsedDate(rs.getLastUsedDate())
                    .expiryDate(rs.getExpiryDate())
                    .build();
        }).toList();
    }

}
