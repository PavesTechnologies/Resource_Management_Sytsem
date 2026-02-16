package com.service_imple.skill_service_impl;

import com.global_exception_handler.SkillTaxonomyExceptionHandler;
import com.service_interface.skill_service_interface.ResourceSkillService;
import com.dto.skill_dto.ResourceSkillBulkRequestDTO;
import com.dto.skill_dto.ResourceSkillProfileResponseDTO;
import com.dto.skill_dto.SkillWithSubSkillDTO;
import com.dto.skill_dto.SubSkillDTO;
import com.entity.skill_entities.ProficiencyLevel;
import com.entity.skill_entities.ResourceSkill;
import com.entity.skill_entities.ResourceSubSkill;
import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SubSkill;
import com.repo.skill_repo.ProficiencyLevelRepository;
import com.repo.skill_repo.ResourceSkillRepository;
import com.repo.skill_repo.ResourceSubSkillRepository;
import com.repo.skill_repo.SkillRepository;
import com.repo.skill_repo.SubSkillRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceSkillServiceImpl implements ResourceSkillService {
    private final ResourceSkillRepository resourceSkillRepository;
    private final ResourceSubSkillRepository resourceSubSkillRepository;
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
        List<ResourceSubSkill> resourceSubSkills = new ArrayList<>();
        
        for (SkillWithSubSkillDTO skillDTO : dto.getSkills()) {
            // Add skill-level proficiency
            ResourceSkill skillResource = ResourceSkill.builder()
                    .resourceId(dto.getResourceId())
                    .skillId(skillDTO.getSkillId())
                    .proficiencyId(skillDTO.getProficiencyId())
                    .lastUsedDate(LocalDate.now())
                    .expiryDate(null)
                    .activeFlag(true)
                    .build();
            resourceSkills.add(skillResource);
            
            // Add sub-skill proficiencies if any
            if (skillDTO.getSubSkills() != null) {
                for (SubSkillDTO subSkillDTO : skillDTO.getSubSkills()) {
                    ResourceSubSkill subSkillResource = ResourceSubSkill.builder()
                            .resourceId(dto.getResourceId())
                            .subSkillId(subSkillDTO.getSubSkillId())
                            .proficiencyId(subSkillDTO.getProficiencyId())
                            .lastUsedDate(LocalDate.now())
                            .expiryDate(null)
                            .activeFlag(true)
                            .build();
                    resourceSubSkills.add(subSkillResource);
                }
            }
        }
        
        resourceSkillRepository.saveAll(resourceSkills);
        resourceSubSkillRepository.saveAll(resourceSubSkills);
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
            
            // Prevent duplicate skill assignment
            boolean skillExists = resourceSkillRepository
                    .existsByResourceIdAndSkillId(
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
    
    private void validateSubSkill(Long resourceId, UUID skillId, SubSkillDTO subSkillDTO, 
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
        
        // Validate subSkill proficiency <= skill proficiency (using display order if available, otherwise skip this validation)
        if (skillProficiency.getDisplayOrder() != null && subSkillProficiency.getDisplayOrder() != null) {
            if (subSkillProficiency.getDisplayOrder() > skillProficiency.getDisplayOrder()) {
                throw new SkillTaxonomyExceptionHandler(
                        "SubSkill proficiency cannot exceed skill proficiency for: " + subSkill.getName());
            }
        }
        
        // Prevent duplicate sub-skill assignment
        boolean subSkillExists = resourceSubSkillRepository
                .existsByResourceIdAndSubSkillId(
                        resourceId,
                        subSkillDTO.getSubSkillId());
        
        if (subSkillExists) {
            throw new SkillTaxonomyExceptionHandler(
                    "SubSkill already assigned to this resource: " + subSkill.getName());
        }
    }

    @Override
    public List<ResourceSkillProfileResponseDTO> getResourceSkillProfile(Long resourceId) {
        List<ResourceSkill> skills = resourceSkillRepository.findByResourceIdAndActiveFlagTrue(resourceId);
        List<ResourceSubSkill> subSkills = resourceSubSkillRepository.findByResourceIdAndActiveFlagTrue(resourceId);
        
        // Group sub-skills by their parent skill
        Map<UUID, List<ResourceSubSkill>> subSkillsBySkill = subSkills.stream()
                .collect(Collectors.groupingBy(rss -> {
                    SubSkill subSkill = subSkillRepository.findById(rss.getSubSkillId()).orElseThrow();
                    return subSkill.getSkill().getId();
                }));
        
        List<ResourceSkillProfileResponseDTO> result = new ArrayList<>();
        
        for (ResourceSkill skillRecord : skills) {
            // Get skill details
            Skill skill = skillRepository.findById(skillRecord.getSkillId()).orElseThrow();
            
            // Get skill proficiency
            ProficiencyLevel skillProficiencyLevel = proficiencyLevelRepository
                    .findById(skillRecord.getProficiencyId()).orElseThrow();
            
            // Get sub-skills for this skill
            List<ResourceSubSkill> skillSubSkills = subSkillsBySkill.getOrDefault(skillRecord.getSkillId(), new ArrayList<>());
            
            // Process sub-skill proficiencies
            List<ResourceSkillProfileResponseDTO.SubSkillProficiencyDTO> subSkillProficiencies = new ArrayList<>();
            for (ResourceSubSkill subSkillRecord : skillSubSkills) {
                SubSkill subSkill = subSkillRepository.findById(subSkillRecord.getSubSkillId()).orElseThrow();
                ProficiencyLevel subProficiencyLevel = proficiencyLevelRepository
                        .findById(subSkillRecord.getProficiencyId()).orElseThrow();
                
                subSkillProficiencies.add(ResourceSkillProfileResponseDTO.SubSkillProficiencyDTO.builder()
                        .subSkill(subSkill.getName())
                        .proficiency(subProficiencyLevel.getProficiencyName())
                        .proficiencyCode(subProficiencyLevel.getProficiencyCode())
                        .build());
            }
            
            // Create response DTO
            ResourceSkillProfileResponseDTO responseDTO = ResourceSkillProfileResponseDTO.builder()
                    .category(skill.getCategory().getName())
                    .skill(skill.getName())
                    .skillProficiency(skillProficiencyLevel.getProficiencyName())
                    .skillProficiencyCode(skillProficiencyLevel.getProficiencyCode())
                    .subSkills(subSkillProficiencies)
                    .lastUsedDate(skillRecord.getLastUsedDate())
                    .expiryDate(skillRecord.getExpiryDate())
                    .build();
            
            result.add(responseDTO);
        }
        
        return result;
    }

}
