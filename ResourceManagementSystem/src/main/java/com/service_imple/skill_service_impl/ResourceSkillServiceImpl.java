package com.service_imple.skill_service_impl;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity.project_entities.Project;
import com.global_exception_handler.SkillTaxonomyExceptionHandler;
import com.repo.allocation_repo.AllocationRepository;
import com.service_interface.skill_service_interface.ResourceSkillService;
import com.dto.skill_dto.ResourceSkillBulkRequestDTO;
import com.dto.skill_dto.ResourceSkillProfileResponseDTO;
import com.dto.skill_dto.ResourceSkillRequestDTO;
import com.dto.skill_dto.ResourceSubSkillRequestDTO;
import com.dto.skill_dto.SkillWithSubSkillDTO;
import com.dto.skill_dto.SubSkillDTO;
import com.entity.skill_entities.ProficiencyLevel;
import com.entity.skill_entities.ResourceSkill;
import com.entity.skill_entities.ResourceSubSkill;
import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SubSkill;
import com.entity.resource_entities.Resource;
import com.repo.skill_repo.ProficiencyLevelRepository;
import com.repo.skill_repo.ResourceSkillRepository;
import com.repo.skill_repo.ResourceSubSkillRepository;
import com.repo.skill_repo.SkillRepository;
import com.repo.skill_repo.SubSkillRepository;
import com.repo.resource_repo.ResourceRepository;
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
    private final ResourceRepository resourceRepository;
    private final AllocationRepository allocationRepository;

    @Override
    @Transactional
    public String addSkillsToResource(ResourceSkillBulkRequestDTO dto) {
        // Validate resource exists and is active before proceeding
        validateResourceExistsAndActive(dto.getResourceId());
        
        // Validate all skills before saving (atomic validation)
        validateAllSkills(dto);
        
        // Save all validated skills
        List<ResourceSkill> resourceSkills = new ArrayList<>();
        List<ResourceSubSkill> resourceSubSkills = new ArrayList<>();
        
        for (SkillWithSubSkillDTO skillDTO : dto.getSkills()) {
            // Add skill-level proficiency
            ResourceSkill skillResource = ResourceSkill.builder()
                    .resourceId(dto.getResourceId())
                    .skill(skillRepository.getReferenceById(skillDTO.getSkillId()))
                    .proficiencyId(skillDTO.getProficiencyId())
                    .lastUsedDate(LocalDate.now())
                    .activeFlag(true)
                    .build();
            resourceSkills.add(skillResource);
            
            // Add sub-skill proficiencies if any
            if (skillDTO.getSubSkills() != null) {
                for (SubSkillDTO subSkillDTO : skillDTO.getSubSkills()) {
                    ResourceSubSkill subSkillResource = ResourceSubSkill.builder()
                            .resourceId(dto.getResourceId())
                            .subSkill(subSkillRepository.getReferenceById(subSkillDTO.getSubSkillId()))
                            .proficiencyId(subSkillDTO.getProficiencyId())
                            .lastUsedDate(LocalDate.now())
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
                .collect(Collectors.groupingBy(rss -> rss.getSubSkill().getSkill().getId()));
        
        List<ResourceSkillProfileResponseDTO> result = new ArrayList<>();
        
        for (ResourceSkill skillRecord : skills) {
            // Get skill details
            Skill skill = skillRecord.getSkill();
            
            // Get skill proficiency
            ProficiencyLevel skillProficiencyLevel = proficiencyLevelRepository
                    .findById(skillRecord.getProficiencyId()).orElseThrow();
            
            // Get sub-skills for this skill
            List<ResourceSubSkill> skillSubSkills = subSkillsBySkill.getOrDefault(skillRecord.getSkill().getId(), new ArrayList<>());
            
            // Process sub-skill proficiencies
            List<ResourceSkillProfileResponseDTO.SubSkillProficiencyDTO> subSkillProficiencies = new ArrayList<>();
            for (ResourceSubSkill subSkillRecord : skillSubSkills) {
                SubSkill subSkill = subSkillRecord.getSubSkill();
                ProficiencyLevel subProficiencyLevel = proficiencyLevelRepository
                        .findById(subSkillRecord.getProficiencyId()).orElseThrow();
                
                subSkillProficiencies.add(ResourceSkillProfileResponseDTO.SubSkillProficiencyDTO.builder()
                        .resourceSubSkillId(subSkillRecord.getId())
                        .subSkill(subSkill.getName())
                        .proficiency(subProficiencyLevel.getProficiencyName())
                        .proficiencyCode(subProficiencyLevel.getProficiencyCode())
                        .build());
            }
            
            // Create response DTO
            ResourceSkillProfileResponseDTO responseDTO = ResourceSkillProfileResponseDTO.builder()
                    .resourceSkillId(skillRecord.getId())
                    .category(skill.getCategory().getName())
                    .skill(skill.getName())
                    .skillProficiency(skillProficiencyLevel.getProficiencyName())
                    .skillProficiencyCode(skillProficiencyLevel.getProficiencyCode())
                    .subSkills(subSkillProficiencies)
                    .lastUsedDate(skillRecord.getLastUsedDate())
                    .build();
            
            result.add(responseDTO);
        }
        
        return result;
    }

    @Override
    @Transactional
    public String addSingleSkillToResource(ResourceSkillRequestDTO dto) {
        // Validate resource exists and is active before proceeding
        validateResourceExistsAndActive(dto.getResourceId());
        
        // Validate skill exists and is ACTIVE
        Skill skill = skillRepository.findById(dto.getSkillId())
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                        "Skill not found: " + dto.getSkillId()));
        
        if (!"ACTIVE".equalsIgnoreCase(skill.getStatus())) {
            throw new SkillTaxonomyExceptionHandler(
                    "Skill is not active: " + skill.getName());
        }
        
        // Validate proficiency exists and is ACTIVE
        ProficiencyLevel proficiency = proficiencyLevelRepository
                .findById(dto.getProficiencyId())
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                        "Proficiency not found: " + dto.getProficiencyId()));
        
        if (!Boolean.TRUE.equals(proficiency.getActiveFlag())) {
            throw new SkillTaxonomyExceptionHandler(
                    "Proficiency level is inactive: " + proficiency.getProficiencyName());
        }
        
        // Check if skill already assigned to resource
        boolean skillExists = resourceSkillRepository
                .existsByResourceIdAndSkillId(dto.getResourceId(), dto.getSkillId());
        
        if (skillExists) {
            throw new SkillTaxonomyExceptionHandler(
                    "Skill already assigned to this resource: " + skill.getName());
        }
        
        // Create and save resource skill
        ResourceSkill resourceSkill = ResourceSkill.builder()
                .resourceId(dto.getResourceId())
                .skill(skillRepository.getReferenceById(dto.getSkillId()))
                .proficiencyId(dto.getProficiencyId())
                .lastUsedDate(LocalDate.now())
                .activeFlag(true)
                .build();
        
        resourceSkillRepository.save(resourceSkill);
        return "Skill successfully added to resource";
    }

    @Override
    @Transactional
    public String addSingleSubSkillToResource(ResourceSubSkillRequestDTO dto) {
        // Validate resource exists and is active before proceeding
        validateResourceExistsAndActive(dto.getResourceId());
        
        // Validate sub-skill exists and is ACTIVE
        SubSkill subSkill = subSkillRepository.findById(dto.getSubSkillId())
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                        "SubSkill not found: " + dto.getSubSkillId()));
        
        if (!"ACTIVE".equalsIgnoreCase(subSkill.getStatus())) {
            throw new SkillTaxonomyExceptionHandler(
                    "SubSkill is not active: " + subSkill.getName());
        }
        
        // Validate proficiency exists and is ACTIVE
        ProficiencyLevel proficiency = proficiencyLevelRepository
                .findById(dto.getProficiencyId())
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                        "Proficiency not found: " + dto.getProficiencyId()));
        
        if (!Boolean.TRUE.equals(proficiency.getActiveFlag())) {
            throw new SkillTaxonomyExceptionHandler(
                    "Proficiency level is inactive: " + proficiency.getProficiencyName());
        }
        
        // Check if sub-skill already assigned to resource
        boolean subSkillExists = resourceSubSkillRepository
                .existsByResourceIdAndSubSkillId(dto.getResourceId(), dto.getSubSkillId());
        
        if (subSkillExists) {
            throw new SkillTaxonomyExceptionHandler(
                    "SubSkill already assigned to this resource: " + subSkill.getName());
        }
        
        // Create and save resource sub-skill
        ResourceSubSkill resourceSubSkill = ResourceSubSkill.builder()
                .resourceId(dto.getResourceId())
                .subSkill(subSkillRepository.getReferenceById(dto.getSubSkillId()))
                .proficiencyId(dto.getProficiencyId())
                .lastUsedDate(LocalDate.now())
                .activeFlag(true)
                .build();
        
        resourceSubSkillRepository.save(resourceSubSkill);
        return "SubSkill successfully added to resource";
    }

    /**
     * Validates that a resource exists and is active before allowing skill assignments
     * @param resourceId The resource ID to validate
     * @throws SkillTaxonomyExceptionHandler if resource doesn't exist or is not active
     */
    private void validateResourceExistsAndActive(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                        "Resource not found with ID: " + resourceId));
        
        if (!Boolean.TRUE.equals(resource.getActiveFlag())) {
            throw new SkillTaxonomyExceptionHandler(
                    "Resource is not active: " + resource.getFullName() + " (ID: " + resourceId + ")");
        }
    }

    @Override
    public List<ResourceSkill> getAllResourceSkills() {
        return resourceSkillRepository.findAllWithSkills();
    }

    @Override
    public List<ResourceSubSkill> getAllResourceSubSkills() {
        return resourceSubSkillRepository.findAllWithSubSkills();
    }

    @Override
    public List<ResourceSkill> getAllResourceSkills(Long resourceId) {
        return resourceSkillRepository.findByResourceId(resourceId);
    }

    @Override
    public List<ResourceSubSkill> getAllResourceSubSkills(Long resourceId) {
        return resourceSubSkillRepository.findByResourceId(resourceId);
    }

    @Override
    public ResourceSkill getResourceSkillById(UUID resourceSkillId) {
        return resourceSkillRepository.findById(resourceSkillId)
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                        "Resource skill not found with ID: " + resourceSkillId));
    }

    @Override
    public ResourceSubSkill getResourceSubSkillById(UUID resourceSubSkillId) {
        return resourceSubSkillRepository.findById(resourceSubSkillId)
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                        "Resource sub-skill not found with ID: " + resourceSubSkillId));
    }

    @Override
    @Transactional
    public ResourceSkill updateResourceSkill(UUID resourceSkillId, ResourceSkillRequestDTO dto) {
        // Find the existing resource skill
        ResourceSkill existingResourceSkill = resourceSkillRepository.findById(resourceSkillId)
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                        "Resource skill not found with ID: " + resourceSkillId));
        
        // Validate resource exists and is active
        validateResourceExistsAndActive(dto.getResourceId());
        
        // If changing skill, validate the new skill
        if (!existingResourceSkill.getSkill().getId().equals(dto.getSkillId())) {
            Skill skill = skillRepository.findById(dto.getSkillId())
                    .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                            "Skill not found: " + dto.getSkillId()));
            
            if (!"ACTIVE".equalsIgnoreCase(skill.getStatus())) {
                throw new SkillTaxonomyExceptionHandler(
                        "Skill is not active: " + skill.getName());
            }
            
            // Check if new skill already assigned to this resource
            boolean skillExists = resourceSkillRepository
                    .existsByResourceIdAndSkillIdAndIdNot(dto.getResourceId(), dto.getSkillId(), resourceSkillId);
            
            if (skillExists) {
                throw new SkillTaxonomyExceptionHandler(
                        "Skill already assigned to this resource: " + skill.getName());
            }
            
            existingResourceSkill.setSkill(skill);
        }
        
        // Validate proficiency exists and is ACTIVE
        ProficiencyLevel proficiency = proficiencyLevelRepository
                .findById(dto.getProficiencyId())
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                        "Proficiency not found: " + dto.getProficiencyId()));
        
        if (!Boolean.TRUE.equals(proficiency.getActiveFlag())) {
            throw new SkillTaxonomyExceptionHandler(
                    "Proficiency level is inactive: " + proficiency.getProficiencyName());
        }
        
        // Update skill fields
        existingResourceSkill.setResourceId(dto.getResourceId());
        existingResourceSkill.setProficiencyId(dto.getProficiencyId());
        // Only set lastUsedDate if explicitly provided in request (can be null to clear it)
        existingResourceSkill.setLastUsedDate(dto.getLastUsedDate());
        if (dto.getActiveFlag() != null) {
            existingResourceSkill.setActiveFlag(dto.getActiveFlag());
        }
        
        // Handle sub-skills update
        if (dto.getSubSkills() != null && !dto.getSubSkills().isEmpty()) {
            updateSubSkillsForResourceSkill(existingResourceSkill.getResourceId(), 
                    existingResourceSkill.getSkill().getId(), dto.getSubSkills());
        }
        
        return resourceSkillRepository.save(existingResourceSkill);
    }
    
    private void updateSubSkillsForResourceSkill(Long resourceId, UUID skillId, 
            List<ResourceSkillRequestDTO.SubSkillUpdateDTO> subSkillUpdates) {
        
        for (ResourceSkillRequestDTO.SubSkillUpdateDTO subSkillDTO : subSkillUpdates) {
            // Validate sub-skill exists and belongs to the skill
            SubSkill subSkill = subSkillRepository.findById(subSkillDTO.getSubSkillId())
                    .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                            "SubSkill not found: " + subSkillDTO.getSubSkillId()));
            
            if (!skillId.equals(subSkill.getSkill().getId())) {
                throw new SkillTaxonomyExceptionHandler(
                        "SubSkill does not belong to the specified skill: " + subSkill.getName());
            }
            
            if (!"ACTIVE".equalsIgnoreCase(subSkill.getStatus())) {
                throw new SkillTaxonomyExceptionHandler(
                        "SubSkill is not active: " + subSkill.getName());
            }
            
            // Validate proficiency exists and is ACTIVE
            ProficiencyLevel proficiency = proficiencyLevelRepository
                    .findById(subSkillDTO.getProficiencyId())
                    .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                            "Proficiency not found for subSkill: " + subSkillDTO.getProficiencyId()));
            
            if (!Boolean.TRUE.equals(proficiency.getActiveFlag())) {
                throw new SkillTaxonomyExceptionHandler(
                        "Proficiency level is inactive for subSkill: " + proficiency.getProficiencyName());
            }
            
            // Find existing resource sub-skill or create new one
            ResourceSubSkill existingResourceSubSkill = resourceSubSkillRepository
                    .findByResourceIdAndSubSkillId(resourceId, subSkillDTO.getSubSkillId())
                    .orElse(null);
            
            if (existingResourceSubSkill != null) {
                // Update existing sub-skill
                existingResourceSubSkill.setProficiencyId(subSkillDTO.getProficiencyId());
                // Only set lastUsedDate if explicitly provided in request (can be null to clear it)
                existingResourceSubSkill.setLastUsedDate(subSkillDTO.getLastUsedDate());
                if (subSkillDTO.getActiveFlag() != null) {
                    existingResourceSubSkill.setActiveFlag(subSkillDTO.getActiveFlag());
                }
                resourceSubSkillRepository.save(existingResourceSubSkill);
            } else {
                // Create new sub-skill assignment
                ResourceSubSkill newResourceSubSkill = ResourceSubSkill.builder()
                        .resourceId(resourceId)
                        .subSkill(subSkillRepository.getReferenceById(subSkillDTO.getSubSkillId()))
                        .proficiencyId(subSkillDTO.getProficiencyId())
                        .lastUsedDate(subSkillDTO.getLastUsedDate())
                        .activeFlag(subSkillDTO.getActiveFlag() != null ? subSkillDTO.getActiveFlag() : true)
                        .build();
                resourceSubSkillRepository.save(newResourceSubSkill);
            }
        }
    }
    
    @Override
    @Transactional
    public String deleteResourceSkill(UUID resourceSkillId) {
        // Find the existing resource skill
        ResourceSkill existingResourceSkill = resourceSkillRepository.findById(resourceSkillId)
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                        "Resource skill not found with ID: " + resourceSkillId));
        
        // Delete associated sub-skills first
        List<ResourceSubSkill> associatedSubSkills = resourceSubSkillRepository
                .findByResourceId(existingResourceSkill.getResourceId());
        
        // Filter sub-skills that belong to this skill
        List<ResourceSubSkill> subSkillsToDelete = associatedSubSkills.stream()
                .filter(rss -> rss.getSubSkill().getSkill().getId().equals(existingResourceSkill.getSkill().getId()))
                .collect(Collectors.toList());
        
        resourceSubSkillRepository.deleteAll(subSkillsToDelete);
        
        // Delete the main resource skill
        resourceSkillRepository.delete(existingResourceSkill);
        
        return "Resource skill and associated sub-skills deleted successfully";
    }
    
    @Override
    @Transactional
    public String deleteResourceSubSkill(UUID resourceSubSkillId) {
        // Find the existing resource sub-skill
        ResourceSubSkill existingResourceSubSkill = resourceSubSkillRepository.findById(resourceSubSkillId)
                .orElseThrow(() -> new SkillTaxonomyExceptionHandler(
                        "Resource sub-skill not found with ID: " + resourceSubSkillId));
        
        // Delete the resource sub-skill
        resourceSubSkillRepository.delete(existingResourceSubSkill);
        
        return "Resource sub-skill deleted successfully";
    }
}
