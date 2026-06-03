package com.service_imple.skill_service_impl;

import com.dto.skill_dto.*;
import com.global_exception_handler.SkillExceptionHandler;
import com.repo.allocation_repo.AllocationRepository;
import com.service_interface.skill_service_interface.ResourceSkillService;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
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

    // -------------------------------------------------------------------------
    // EmployeeSkillMapping migration: bulk upsert via EmployeeSkillsRequestDto
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

            boolean skillActiveFlag = !"INACTIVE".equalsIgnoreCase(skillDto.getStatus());

            if (skillDto.getSubSkills() == null || skillDto.getSubSkills().isEmpty()) {
                upsertResourceSkill(requestDto.getResourceId(), skill,
                        skillDto.getProficiencyId(), skillActiveFlag);
            } else {
                upsertResourceSkill(requestDto.getResourceId(), skill,
                        skillDto.getProficiencyId(), skillActiveFlag);

                for (SubSkillMappingItemDto subSkillDto : skillDto.getSubSkills()) {
                    if (subSkillDto.getSubSkillId() == null) {
                        throw SkillExceptionHandler.badRequest(
                                "subSkillId is required for every subSkill entry");
                    }
                    SubSkill subSkill = subSkillRepository.findById(subSkillDto.getSubSkillId())
                            .orElseThrow(() -> SkillExceptionHandler.notFound(
                                    "SubSkill not found: " + subSkillDto.getSubSkillId()));

                    UUID subProficiencyId = subSkillDto.getProficiencyId() != null
                            ? subSkillDto.getProficiencyId() : skillDto.getProficiencyId();
                    boolean subActiveFlag = subSkillDto.getStatus() != null
                            ? !"INACTIVE".equalsIgnoreCase(subSkillDto.getStatus()) : skillActiveFlag;

                    upsertResourceSubSkill(requestDto.getResourceId(), subSkill,
                            subProficiencyId, subActiveFlag);
                }
            }
        }
        return "Skills mapped successfully";
    }

    // -------------------------------------------------------------------------
    // EmployeeSkillMapping migration: single-skill save (used by approval flow)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public String saveSkillMapping(EmployeeSkillRequestDto requestDto) {
        if (requestDto.getEmployeeId() == null || requestDto.getEmployeeId().isBlank()) {
            throw SkillExceptionHandler.badRequest("Employee ID is required");
        }
        if (requestDto.getSkillId() == null) {
            throw SkillExceptionHandler.badRequest("Skill ID is required");
        }

        Skill skill = skillRepository.findById(requestDto.getSkillId())
                .orElseThrow(() -> SkillExceptionHandler.notFound("Skill not found"));

        boolean activeFlag = !"INACTIVE".equalsIgnoreCase(requestDto.getStatus());

        if (requestDto.getSubskillId() == null) {
            if (resourceSkillRepository.existsByResourceIdAndSkillId(
                    requestDto.getEmployeeId(), requestDto.getSkillId())) {
                throw SkillExceptionHandler.conflict("Skill already mapped to this employee");
            }
            ResourceSkill rs = ResourceSkill.builder()
                    .resourceId(requestDto.getEmployeeId())
                    .skill(skill)
                    .proficiencyId(requestDto.getProficiencyId())
                    .lastUsedDate(LocalDate.now())
                    .activeFlag(activeFlag)
                    .build();
            resourceSkillRepository.save(rs);
        } else {
            SubSkill subSkill = subSkillRepository.findById(requestDto.getSubskillId())
                    .orElseThrow(() -> SkillExceptionHandler.notFound("SubSkill not found"));

            if (resourceSubSkillRepository.existsByResourceIdAndSubSkillId(
                    requestDto.getEmployeeId(), requestDto.getSubskillId())) {
                throw SkillExceptionHandler.conflict("Skill already mapped to this employee");
            }

            if (!resourceSkillRepository.existsByResourceIdAndSkillId(
                    requestDto.getEmployeeId(), requestDto.getSkillId())) {
                ResourceSkill rs = ResourceSkill.builder()
                        .resourceId(requestDto.getEmployeeId())
                        .skill(skill)
                        .proficiencyId(requestDto.getProficiencyId())
                        .lastUsedDate(LocalDate.now())
                        .activeFlag(activeFlag)
                        .build();
                resourceSkillRepository.save(rs);
            }

            ResourceSubSkill rss = ResourceSubSkill.builder()
                    .resourceId(requestDto.getEmployeeId())
                    .subSkill(subSkill)
                    .proficiencyId(requestDto.getProficiencyId())
                    .lastUsedDate(LocalDate.now())
                    .activeFlag(activeFlag)
                    .build();
            resourceSubSkillRepository.save(rss);
        }
        return "Skill mapped successfully";
    }

    // -------------------------------------------------------------------------
    // EmployeeSkillMapping migration: flat skill list for a resource
    // -------------------------------------------------------------------------

    @Override
    public List<EmployeeSkillResponseDto> getEmployeeSkills(String resourceId) {
        List<ResourceSkill> skills = resourceSkillRepository.findByResourceIdAndActiveFlagTrue(resourceId);
        List<ResourceSubSkill> subSkills = resourceSubSkillRepository.findByResourceIdAndActiveFlagTrue(resourceId);

        // Collect the skill IDs that have at least one active subskill entry.
        // For those skills we emit only the subskill rows (matches original behaviour
        // where a skill+subskill combo never produced an extra skill-only row).
        Set<UUID> skillIdsWithSubSkills = subSkills.stream()
                .map(rss -> rss.getSubSkill().getSkill().getId())
                .collect(Collectors.toSet());

        List<EmployeeSkillResponseDto> result = new ArrayList<>();

        // Skill-only rows: only when no active subskills exist for that skill
        for (ResourceSkill rs : skills) {
            if (skillIdsWithSubSkills.contains(rs.getSkill().getId())) {
                continue;
            }
            Skill skill = rs.getSkill();
            ProficiencyLevel proficiency = rs.getProficiencyId() != null
                    ? proficiencyLevelRepository.findById(rs.getProficiencyId()).orElse(null) : null;

            result.add(EmployeeSkillResponseDto.builder()
                    .id(rs.getId())
                    .employeeId(rs.getResourceId())
                    .categoryName(skill.getCategory() != null ? skill.getCategory().getName() : null)
                    .skillName(skill.getName())
                    .subskillName(null)
                    .proficiency(proficiency != null ? proficiency.getProficiencyName() : null)
                    .status(Boolean.TRUE.equals(rs.getActiveFlag()) ? "ACTIVE" : "INACTIVE")
                    .build());
        }

        // Subskill rows: one entry per active ResourceSubSkill
        for (ResourceSubSkill rss : subSkills) {
            SubSkill subSkill = rss.getSubSkill();
            Skill parentSkill = subSkill.getSkill();
            ProficiencyLevel proficiency = rss.getProficiencyId() != null
                    ? proficiencyLevelRepository.findById(rss.getProficiencyId()).orElse(null) : null;

            result.add(EmployeeSkillResponseDto.builder()
                    .id(rss.getId())
                    .employeeId(rss.getResourceId())
                    .categoryName(parentSkill != null && parentSkill.getCategory() != null
                            ? parentSkill.getCategory().getName() : null)
                    .skillName(parentSkill != null ? parentSkill.getName() : null)
                    .subskillName(subSkill.getName())
                    .proficiency(proficiency != null ? proficiency.getProficiencyName() : null)
                    .status(Boolean.TRUE.equals(rss.getActiveFlag()) ? "ACTIVE" : "INACTIVE")
                    .build());
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Upsert helpers for saveSkillMappings
    // -------------------------------------------------------------------------

    private void upsertResourceSkill(String resourceId, Skill skill, UUID proficiencyId, boolean activeFlag) {
        resourceSkillRepository.findByResourceIdAndSkillId(resourceId, skill.getId())
                .ifPresentOrElse(existing -> {
                    existing.setProficiencyId(proficiencyId);
                    existing.setActiveFlag(activeFlag);
                    resourceSkillRepository.save(existing);
                }, () -> {
                    ResourceSkill rs = ResourceSkill.builder()
                            .resourceId(resourceId)
                            .skill(skill)
                            .proficiencyId(proficiencyId)
                            .lastUsedDate(LocalDate.now())
                            .activeFlag(activeFlag)
                            .build();
                    resourceSkillRepository.save(rs);
                });
    }

    private void upsertResourceSubSkill(String resourceId, SubSkill subSkill,
                                        UUID proficiencyId, boolean activeFlag) {
        resourceSubSkillRepository.findByResourceIdAndSubSkillId(resourceId, subSkill.getId())
                .ifPresentOrElse(existing -> {
                    existing.setProficiencyId(proficiencyId);
                    existing.setActiveFlag(activeFlag);
                    resourceSubSkillRepository.save(existing);
                }, () -> {
                    ResourceSubSkill rss = ResourceSubSkill.builder()
                            .resourceId(resourceId)
                            .subSkill(subSkill)
                            .proficiencyId(proficiencyId)
                            .lastUsedDate(LocalDate.now())
                            .activeFlag(activeFlag)
                            .build();
                    resourceSubSkillRepository.save(rss);
                });
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "resource-skills", key = "#dto.resourceId"),
        @CacheEvict(value = "resource-timelines", allEntries = true),
        @CacheEvict(value = "bench-matches", allEntries = true)
    })
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
                            .subSkill(subSkillRepository.getReferenceById(subSkillDTO.getProficiencyId()))
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
                    .orElseThrow(() -> SkillExceptionHandler.badRequest(
                            "Skill not found: " + skillDTO.getSkillId()));
            
            if (!"ACTIVE".equalsIgnoreCase(skill.getStatus())) {
                throw SkillExceptionHandler.badRequest(
                        "Skill is not active: " + skill.getName());
            }
            
            // Validate skill proficiency exists and is ACTIVE
            ProficiencyLevel skillProficiency = proficiencyLevelRepository
                    .findById(skillDTO.getProficiencyId())
                    .orElseThrow(() -> SkillExceptionHandler.badRequest(
                            "Proficiency not found: " + skillDTO.getProficiencyId()));
            
            if (!Boolean.TRUE.equals(skillProficiency.getActiveFlag())) {
                throw SkillExceptionHandler.badRequest(
                        "Proficiency level is inactive: " + skillProficiency.getProficiencyName());
            }
            
            // Prevent duplicate skill assignment
            boolean skillExists = resourceSkillRepository
                    .existsByResourceIdAndSkillId(
                            dto.getResourceId(),
                            skillDTO.getSkillId());
            
            if (skillExists) {
                throw SkillExceptionHandler.badRequest(
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
    
    private void validateSubSkill(String resourceId, UUID skillId, SubSkillDTO subSkillDTO,
                                 ProficiencyLevel skillProficiency) {
        // Validate subSkill exists
        SubSkill subSkill = subSkillRepository.findById(subSkillDTO.getSubSkillId())
                .orElseThrow(() -> SkillExceptionHandler.badRequest(
                        "SubSkill not found: " + subSkillDTO.getSubSkillId()));
        
        // Validate subSkill belongs to Skill
        if (!skillId.equals(subSkill.getSkill().getId())) {
            throw SkillExceptionHandler.badRequest(
                    "SubSkill does not belong to the specified skill: " + subSkill.getName());
        }
        
        // Validate subSkill is ACTIVE
        if (!"ACTIVE".equalsIgnoreCase(subSkill.getStatus())) {
            throw SkillExceptionHandler.badRequest(
                    "SubSkill is not active: " + subSkill.getName());
        }
        
        // Validate subSkill proficiency exists and is ACTIVE
        ProficiencyLevel subSkillProficiency = proficiencyLevelRepository
                .findById(subSkillDTO.getProficiencyId())
                .orElseThrow(() -> SkillExceptionHandler.badRequest(
                        "Proficiency not found for subSkill: " + subSkillDTO.getProficiencyId()));
        
        if (!Boolean.TRUE.equals(subSkillProficiency.getActiveFlag())) {
            throw SkillExceptionHandler.badRequest(
                    "Proficiency level is inactive for subSkill: " + subSkillProficiency.getProficiencyName());
        }
        
        // Validate subSkill proficiency <= skill proficiency (using display order if available, otherwise skip this validation)
        if (skillProficiency.getDisplayOrder() != null && subSkillProficiency.getDisplayOrder() != null) {
            if (subSkillProficiency.getDisplayOrder() > skillProficiency.getDisplayOrder()) {
                throw SkillExceptionHandler.badRequest(
                        "SubSkill proficiency cannot exceed skill proficiency for: " + subSkill.getName());
            }
        }
        
        // Prevent duplicate sub-skill assignment
        boolean subSkillExists = resourceSubSkillRepository
                .existsByResourceIdAndSubSkillId(
                        resourceId,
                        subSkillDTO.getSubSkillId());
        
        if (subSkillExists) {
            throw SkillExceptionHandler.badRequest(
                    "SubSkill already assigned to this resource: " + subSkill.getName());
        }
    }

    @Override
    public List<ResourceSkillProfileResponseDTO> getResourceSkillProfile(String resourceId) {
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
                .orElseThrow(() -> SkillExceptionHandler.badRequest(
                        "Skill not found: " + dto.getSkillId()));
        
        if (!"ACTIVE".equalsIgnoreCase(skill.getStatus())) {
            throw SkillExceptionHandler.badRequest(
                    "Skill is not active: " + skill.getName());
        }
        
        // Validate proficiency exists and is ACTIVE
        ProficiencyLevel proficiency = proficiencyLevelRepository
                .findById(dto.getProficiencyId())
                .orElseThrow(() -> SkillExceptionHandler.badRequest(
                        "Proficiency not found: " + dto.getProficiencyId()));
        
        if (!Boolean.TRUE.equals(proficiency.getActiveFlag())) {
            throw SkillExceptionHandler.badRequest(
                    "Proficiency level is inactive: " + proficiency.getProficiencyName());
        }
        
        // Check if skill already assigned to resource
        boolean skillExists = resourceSkillRepository
                .existsByResourceIdAndSkillId(dto.getResourceId(), dto.getSkillId());
        
        if (skillExists) {
            throw SkillExceptionHandler.badRequest(
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
                .orElseThrow(() -> SkillExceptionHandler.badRequest(
                        "SubSkill not found: " + dto.getSubSkillId()));
        
        if (!"ACTIVE".equalsIgnoreCase(subSkill.getStatus())) {
            throw SkillExceptionHandler.badRequest(
                    "SubSkill is not active: " + subSkill.getName());
        }
        
        // Validate proficiency exists and is ACTIVE
        ProficiencyLevel proficiency = proficiencyLevelRepository
                .findById(dto.getProficiencyId())
                .orElseThrow(() -> SkillExceptionHandler.badRequest(
                        "Proficiency not found: " + dto.getProficiencyId()));
        
        if (!Boolean.TRUE.equals(proficiency.getActiveFlag())) {
            throw SkillExceptionHandler.badRequest(
                    "Proficiency level is inactive: " + proficiency.getProficiencyName());
        }
        
        // Check if sub-skill already assigned to resource
        boolean subSkillExists = resourceSubSkillRepository
                .existsByResourceIdAndSubSkillId(dto.getResourceId(), dto.getSubSkillId());
        
        if (subSkillExists) {
            throw SkillExceptionHandler.badRequest(
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
     * @throws //SkillTaxonomyExceptionHandler if resource doesn't exist or is not active
     */
    private void validateResourceExistsAndActive(String resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> SkillExceptionHandler.badRequest(
                        "Resource not found with ID: " + resourceId));
        
        if (!Boolean.TRUE.equals(resource.getActiveFlag())) {
            throw SkillExceptionHandler.badRequest(
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
    @Cacheable(value = "resource-skills", key = "#resourceId")
    public List<ResourceSkill> getAllResourceSkills(String resourceId) {
        return resourceSkillRepository.findByResourceId(resourceId);
    }

    @Override
    public List<ResourceSubSkill> getAllResourceSubSkills(String resourceId) {
        return resourceSubSkillRepository.findByResourceId(resourceId);
    }

    @Override
    public ResourceSkill getResourceSkillById(UUID resourceSkillId) {
        return resourceSkillRepository.findById(resourceSkillId)
                .orElseThrow(() -> SkillExceptionHandler.badRequest(
                        "Resource skill not found with ID: " + resourceSkillId));
    }

    @Override
    public ResourceSubSkill getResourceSubSkillById(UUID resourceSubSkillId) {
        return resourceSubSkillRepository.findById(resourceSubSkillId)
                .orElseThrow(() -> SkillExceptionHandler.badRequest(
                        "Resource sub-skill not found with ID: " + resourceSubSkillId));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "resource-skills", key = "#dto.resourceId"),
        @CacheEvict(value = "resource-timelines", allEntries = true),
        @CacheEvict(value = "bench-matches", allEntries = true)
    })
    public ResourceSkill updateResourceSkill(UUID resourceSkillId, ResourceSkillRequestDTO dto) {
        // Find the existing resource skill
        ResourceSkill existingResourceSkill = resourceSkillRepository.findById(resourceSkillId)
                .orElseThrow(() -> SkillExceptionHandler.badRequest(
                        "Resource skill not found with ID: " + resourceSkillId));
        
        // Validate resource exists and is active
        validateResourceExistsAndActive(dto.getResourceId());
        
        // If changing skill, validate the new skill
        if (!existingResourceSkill.getSkill().getId().equals(dto.getSkillId())) {
            Skill skill = skillRepository.findById(dto.getSkillId())
                    .orElseThrow(() -> SkillExceptionHandler.badRequest(
                            "Skill not found: " + dto.getSkillId()));
            
            if (!"ACTIVE".equalsIgnoreCase(skill.getStatus())) {
                throw SkillExceptionHandler.badRequest(
                        "Skill is not active: " + skill.getName());
            }
            
            // Check if new skill already assigned to this resource
            boolean skillExists = resourceSkillRepository
                    .existsByResourceIdAndSkillIdAndIdNot(dto.getResourceId(), dto.getSkillId(), resourceSkillId);
            
            if (skillExists) {
                throw SkillExceptionHandler.badRequest(
                        "Skill already assigned to this resource: " + skill.getName());
            }
            
            existingResourceSkill.setSkill(skill);
        }
        
        // Validate proficiency exists and is ACTIVE
        ProficiencyLevel proficiency = proficiencyLevelRepository
                .findById(dto.getProficiencyId())
                .orElseThrow(() -> SkillExceptionHandler.badRequest(
                        "Proficiency not found: " + dto.getProficiencyId()));
        
        if (!Boolean.TRUE.equals(proficiency.getActiveFlag())) {
            throw SkillExceptionHandler.badRequest(
                    "Proficiency level is inactive: " + proficiency.getProficiencyName());
        }
        
        // Update skill fields
        existingResourceSkill.setResourceId(dto.getResourceId());
        existingResourceSkill.setProficiencyId(dto.getProficiencyId());
        // Automatically set lastUsedDate to current date and activeFlag to true
        existingResourceSkill.setLastUsedDate(LocalDate.now());
        existingResourceSkill.setActiveFlag(true);
        
        // Handle sub-skills update
        if (dto.getSubSkills() != null && !dto.getSubSkills().isEmpty()) {
            updateSubSkillsForResourceSkill(existingResourceSkill.getResourceId(), 
                    existingResourceSkill.getSkill().getId(), dto.getSubSkills());
        }
        
        return resourceSkillRepository.save(existingResourceSkill);
    }
    
    private void updateSubSkillsForResourceSkill(String resourceId, UUID skillId, 
            List<ResourceSkillRequestDTO.SubSkillUpdateDTO> subSkillUpdates) {
        
        for (ResourceSkillRequestDTO.SubSkillUpdateDTO subSkillDTO : subSkillUpdates) {
            // Validate sub-skill exists and belongs to the skill
            SubSkill subSkill = subSkillRepository.findById(subSkillDTO.getSubSkillId())
                    .orElseThrow(() -> SkillExceptionHandler.badRequest(
                            "SubSkill not found: " + subSkillDTO.getSubSkillId()));
            
            if (!skillId.equals(subSkill.getSkill().getId())) {
                throw SkillExceptionHandler.badRequest(
                        "SubSkill does not belong to the specified skill: " + subSkill.getName());
            }
            
            if (!"ACTIVE".equalsIgnoreCase(subSkill.getStatus())) {
                throw SkillExceptionHandler.badRequest(
                        "SubSkill is not active: " + subSkill.getName());
            }
            
            // Validate proficiency exists and is ACTIVE
            ProficiencyLevel proficiency = proficiencyLevelRepository
                    .findById(subSkillDTO.getProficiencyId())
                    .orElseThrow(() -> SkillExceptionHandler.badRequest(
                            "Proficiency not found for subSkill: " + subSkillDTO.getProficiencyId()));
            
            if (!Boolean.TRUE.equals(proficiency.getActiveFlag())) {
                throw SkillExceptionHandler.badRequest(
                        "Proficiency level is inactive for subSkill: " + proficiency.getProficiencyName());
            }
            
            // Find existing resource sub-skill or create new one
            ResourceSubSkill existingResourceSubSkill = resourceSubSkillRepository
                    .findByResourceIdAndSubSkillId(resourceId, subSkillDTO.getSubSkillId())
                    .orElse(null);
            
            if (existingResourceSubSkill != null) {
                // Update existing sub-skill
                existingResourceSubSkill.setProficiencyId(subSkillDTO.getProficiencyId());
                // Automatically set lastUsedDate to current date and activeFlag to true
                existingResourceSubSkill.setLastUsedDate(LocalDate.now());
                existingResourceSubSkill.setActiveFlag(true);
                resourceSubSkillRepository.save(existingResourceSubSkill);
            } else {
                // Create new sub-skill assignment
                ResourceSubSkill newResourceSubSkill = ResourceSubSkill.builder()
                        .resourceId(resourceId)
                        .subSkill(subSkillRepository.getReferenceById(subSkillDTO.getSubSkillId()))
                        .proficiencyId(subSkillDTO.getProficiencyId())
                        .lastUsedDate(LocalDate.now())
                        .activeFlag(true)
                        .build();
                resourceSubSkillRepository.save(newResourceSubSkill);
            }
        }
    }
    
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "resource-skills", allEntries = true),
        @CacheEvict(value = "resource-timelines", allEntries = true),
        @CacheEvict(value = "bench-matches", allEntries = true)
    })
    public String deleteResourceSkill(UUID resourceSkillId) {
        // Find the existing resource skill
        ResourceSkill existingResourceSkill = resourceSkillRepository.findById(resourceSkillId)
                .orElseThrow(() -> SkillExceptionHandler.badRequest(
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
    @Caching(evict = {
        @CacheEvict(value = "resource-skills",    allEntries = true),
        @CacheEvict(value = "resource-timelines", allEntries = true),
        @CacheEvict(value = "bench-matches",      allEntries = true)
    })
    public String deleteResourceSubSkill(UUID resourceSubSkillId) {
        // Find the existing resource sub-skill
        ResourceSubSkill existingResourceSubSkill = resourceSubSkillRepository.findById(resourceSubSkillId)
                .orElseThrow(() -> SkillExceptionHandler.badRequest(
                        "Resource sub-skill not found with ID: " + resourceSubSkillId));
        
        // Delete the resource sub-skill
        resourceSubSkillRepository.delete(existingResourceSubSkill);
        
        return "Resource sub-skill deleted successfully";
    }
}
