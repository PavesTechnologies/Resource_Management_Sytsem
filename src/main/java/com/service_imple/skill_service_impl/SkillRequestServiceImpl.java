package com.service_imple.skill_service_impl;

import com.dto.skill_dto.*;
import com.entity.skill_entities.*;
import com.entity_enums.skill_enums.SkillRequestStatus;
import com.global_exception_handler.SkillExceptionHandler;
import com.repo.resource_repo.ResourceRepository;
import com.repo.skill_repo.*;
import com.service_interface.skill_service_interface.ResourceSkillService;
import com.service_interface.skill_service_interface.SkillRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillRequestServiceImpl implements SkillRequestService {

    private final SkillRequestRepository skillRequestRepository;
    private final SkillCategoryRepository skillCategoryRepository;
    private final SkillRepository skillRepository;
    private final SubSkillRepository subSkillRepository;
    private final ProficiencyLevelRepository proficiencyLevelRepository;
    private final ResourceSkillService resourceSkillService;
    private final ResourceRepository resourceRepository;

    // -------------------------------------------------------------------------
    // Submit — bulk nested: categories > skills > subSkills
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public String submitSkillRequest(NewSkillRequestDto requestDto) {
        if (requestDto.getResourceId() == null || requestDto.getResourceId().isBlank()) {
            throw SkillExceptionHandler.badRequest("Resource ID is required");
        }
        if (requestDto.getCategories() == null || requestDto.getCategories().isEmpty()) {
            throw SkillExceptionHandler.badRequest("At least one category is required");
        }

        for (SkillCategoryRequestItemDto categoryDto : requestDto.getCategories()) {
            if (categoryDto.getCategoryName() == null || categoryDto.getCategoryName().isBlank()) {
                throw SkillExceptionHandler.badRequest("Category name is required for every category entry");
            }
            if (categoryDto.getSkills() == null || categoryDto.getSkills().isEmpty()) {
                throw SkillExceptionHandler.badRequest(
                        "At least one skill is required for category: " + categoryDto.getCategoryName());
            }

            String categoryName = categoryDto.getCategoryName().trim();

            for (SkillItemRequestDto skillDto : categoryDto.getSkills()) {
                if (skillDto.getSkillName() == null || skillDto.getSkillName().isBlank()) {
                    throw SkillExceptionHandler.badRequest(
                            "Skill name is required for every skill entry in category: " + categoryName);
                }

                String skillName = skillDto.getSkillName().trim();
                ProficiencyLevel skillProficiency = resolveProficiency(skillDto.getProficiencyId());
                String skillStatus = resolveStatus(skillDto.getStatus());

                if (skillDto.getSubSkills() == null || skillDto.getSubSkills().isEmpty()) {
                    // No subskills — save one request row at skill level
                    saveRequestRow(requestDto.getResourceId(), categoryName, skillName,
                            null, skillProficiency, skillStatus);
                } else {
                    for (SubSkillRequestItemDto subSkillDto : skillDto.getSubSkills()) {
                        if (subSkillDto.getSubSkillName() == null || subSkillDto.getSubSkillName().isBlank()) {
                            throw SkillExceptionHandler.badRequest(
                                    "SubSkill name is required for every subSkill entry under skill: " + skillName);
                        }

                        // SubSkill proficiency overrides skill-level; falls back to skill proficiency
                        ProficiencyLevel effectiveProficiency = subSkillDto.getProficiencyId() != null
                                ? resolveProficiency(subSkillDto.getProficiencyId())
                                : skillProficiency;

                        String effectiveStatus = subSkillDto.getStatus() != null
                                ? resolveStatus(subSkillDto.getStatus())
                                : skillStatus;

                        saveRequestRow(requestDto.getResourceId(), categoryName, skillName,
                                subSkillDto.getSubSkillName().trim(), effectiveProficiency, effectiveStatus);
                    }
                }
            }
        }

        return "Skill request submitted successfully";
    }

    // -------------------------------------------------------------------------
    // Fetch all requests (admin view)
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<SkillRequestResponseDto> getAllSkillRequests() {
        List<SkillRequest> requests = skillRequestRepository.findAllWithProficiency();

        // Batch-load all resource names in one query to avoid N+1
        List<String> resourceIds = requests.stream()
                .map(SkillRequest::getResourceId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, String> resourceNameMap = resourceRepository
                .findAllByResourceIdIn(resourceIds)
                .stream()
                .collect(Collectors.toMap(
                        r -> r.getResourceId(),
                        r -> r.getFullName() != null ? r.getFullName() : r.getResourceId()
                ));

        return requests.stream()
                .map(req -> toResponseDto(req, resourceNameMap))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Fetch requests for a single resource
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<SkillRequestResponseDto> getRequestsByResourceId(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            throw SkillExceptionHandler.badRequest("Resource ID is required");
        }

        String resourceName = resourceRepository.findById(resourceId)
                .map(r -> r.getFullName() != null ? r.getFullName() : r.getResourceId())
                .orElse(resourceId);

        Map<String, String> resourceNameMap = Map.of(resourceId, resourceName);

        return skillRequestRepository.findByResourceIdOrderByRequestedAtDesc(resourceId)
                .stream()
                .map(req -> toResponseDto(req, resourceNameMap))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Approve
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public String approveSkillRequest(UUID requestId, String approvedBy) {
        SkillRequest request = skillRequestRepository.findById(requestId)
                .orElseThrow(() -> SkillExceptionHandler.notFound("Skill request not found"));

        if (request.getRequestStatus() != SkillRequestStatus.PENDING) {
            throw SkillExceptionHandler.badRequest("Only PENDING requests can be approved");
        }

        SkillCategory category = findOrCreateCategory(request.getCategoryName());
        Skill skill = findOrCreateSkill(request.getSkillName(), category);

        SubSkill subSkill = null;
        if (request.getSubskillName() != null && !request.getSubskillName().isBlank()) {
            subSkill = findOrCreateSubSkill(request.getSubskillName(), skill);
        }

        EmployeeSkillRequestDto mappingDto = EmployeeSkillRequestDto.builder()
                .employeeId(request.getResourceId())
                .categoryId(category.getId())
                .skillId(skill.getId())
                .subskillId(subSkill != null ? subSkill.getId() : null)
                .proficiencyId(request.getProficiency() != null
                        ? request.getProficiency().getProficiencyId() : null)
                .status("ACTIVE")
                .build();

        resourceSkillService.saveSkillMapping(mappingDto);

        request.setRequestStatus(SkillRequestStatus.APPROVED);
        request.setApprovedAt(LocalDateTime.now());
        request.setApprovedBy(approvedBy);
        skillRequestRepository.save(request);

        return "Skill request approved successfully";
    }

    // -------------------------------------------------------------------------
    // Reject
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public String rejectSkillRequest(UUID requestId, SkillApprovalDto approvalDto) {
        SkillRequest request = skillRequestRepository.findById(requestId)
                .orElseThrow(() -> SkillExceptionHandler.notFound("Skill request not found"));

        if (request.getRequestStatus() != SkillRequestStatus.PENDING) {
            throw SkillExceptionHandler.badRequest("Only PENDING requests can be rejected");
        }

        request.setRequestStatus(SkillRequestStatus.REJECTED);
        if (approvalDto != null && approvalDto.getRemarks() != null) {
            request.setRemarks(approvalDto.getRemarks());
        }
        skillRequestRepository.save(request);

        return "Skill request rejected";
    }

    // -------------------------------------------------------------------------
    // Bulk Approve
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public Map<String, Integer> bulkApproveByEmployee(String resourceId, BulkSkillApprovalRequestDto dto) {
        validateBulkRequest(resourceId, dto);

        List<SkillRequest> requests = skillRequestRepository
                .findByResourceIdAndIdIn(resourceId, dto.getRequestIds());

        String approvedBy = (dto.getReviewedBy() != null && !dto.getReviewedBy().isBlank())
                ? dto.getReviewedBy() : "ADMIN";

        int approvedCount = 0;
        for (SkillRequest request : requests) {
            if (request.getRequestStatus() != SkillRequestStatus.PENDING) {
                continue;
            }

            SkillCategory category = findOrCreateCategory(request.getCategoryName());
            Skill skill = findOrCreateSkill(request.getSkillName(), category);

            SubSkill subSkill = null;
            if (request.getSubskillName() != null && !request.getSubskillName().isBlank()) {
                subSkill = findOrCreateSubSkill(request.getSubskillName(), skill);
            }

            EmployeeSkillRequestDto mappingDto = EmployeeSkillRequestDto.builder()
                    .employeeId(request.getResourceId())
                    .categoryId(category.getId())
                    .skillId(skill.getId())
                    .subskillId(subSkill != null ? subSkill.getId() : null)
                    .proficiencyId(request.getProficiency() != null
                            ? request.getProficiency().getProficiencyId() : null)
                    .status("ACTIVE")
                    .build();

            try {
                resourceSkillService.saveSkillMapping(mappingDto);
            } catch (SkillExceptionHandler ignored) {
                // Mapping already exists — still mark the request APPROVED
            }

            request.setRequestStatus(SkillRequestStatus.APPROVED);
            request.setApprovedAt(LocalDateTime.now());
            request.setApprovedBy(approvedBy);
            if (dto.getRemarks() != null) {
                request.setRemarks(dto.getRemarks());
            }
            skillRequestRepository.save(request);
            approvedCount++;
        }

        return Map.of("approvedCount", approvedCount);
    }

    // -------------------------------------------------------------------------
    // Bulk Reject
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public Map<String, Integer> bulkRejectByEmployee(String resourceId, BulkSkillApprovalRequestDto dto) {
        validateBulkRequest(resourceId, dto);

        List<SkillRequest> requests = skillRequestRepository
                .findByResourceIdAndIdIn(resourceId, dto.getRequestIds());

        String reviewedBy = (dto.getReviewedBy() != null && !dto.getReviewedBy().isBlank())
                ? dto.getReviewedBy() : "ADMIN";

        int rejectedCount = 0;
        for (SkillRequest request : requests) {
            if (request.getRequestStatus() != SkillRequestStatus.PENDING) {
                continue;
            }

            request.setRequestStatus(SkillRequestStatus.REJECTED);
            request.setApprovedAt(LocalDateTime.now());
            request.setApprovedBy(reviewedBy);
            if (dto.getRemarks() != null) {
                request.setRemarks(dto.getRemarks());
            }
            skillRequestRepository.save(request);
            rejectedCount++;
        }

        return Map.of("rejectedCount", rejectedCount);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void validateBulkRequest(String resourceId, BulkSkillApprovalRequestDto dto) {
        if (resourceId == null || resourceId.isBlank()) {
            throw SkillExceptionHandler.badRequest("Resource ID is required");
        }
        if (dto == null || dto.getRequestIds() == null || dto.getRequestIds().isEmpty()) {
            throw SkillExceptionHandler.badRequest("At least one request ID is required");
        }
    }

    private void saveRequestRow(String resourceId, String categoryName, String skillName,
                                String subskillName, ProficiencyLevel proficiency, String status) {
        SkillRequest request = new SkillRequest();
        request.setResourceId(resourceId);
        request.setCategoryName(categoryName);
        request.setSkillName(skillName);
        request.setSubskillName(subskillName);
        request.setProficiency(proficiency);
        request.setRequestStatus(SkillRequestStatus.PENDING);
        skillRequestRepository.save(request);
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

    private SkillCategory findOrCreateCategory(String name) {
        String normalized = name.trim();
        return skillCategoryRepository.findActiveCategories().stream()
                .filter(c -> c.getName().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseGet(() -> {
                    SkillCategory newCategory = new SkillCategory();
                    newCategory.setName(normalized);
                    return skillCategoryRepository.save(newCategory);
                });
    }

    private Skill findOrCreateSkill(String name, SkillCategory category) {
        String normalized = name.trim();
        return skillRepository.findActiveSkillsByCategoryId(category.getId()).stream()
                .filter(s -> s.getName().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseGet(() -> {
                    Skill newSkill = new Skill();
                    newSkill.setName(normalized);
                    newSkill.setCategory(category);
                    return skillRepository.save(newSkill);
                });
    }

    private SubSkill findOrCreateSubSkill(String name, Skill skill) {
        String normalized = name.trim();
        return subSkillRepository.findActiveSubSkillsBySkillId(skill.getId()).stream()
                .filter(ss -> ss.getName().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseGet(() -> {
                    SubSkill newSubSkill = new SubSkill();
                    newSubSkill.setName(normalized);
                    newSubSkill.setSkill(skill);
                    return subSkillRepository.save(newSubSkill);
                });
    }

    // -------------------------------------------------------------------------
    // Nightly purge — called by CentralizedJobScheduler
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void purgeOldResolvedRequests() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(1);
        int deleted = skillRequestRepository.deleteByStatusInAndUpdatedAtBefore(
                List.of(SkillRequestStatus.APPROVED, SkillRequestStatus.REJECTED), cutoff);
        log.info("Purged {} skill request(s) with APPROVED/REJECTED status older than 1 month", deleted);
    }

    private SkillRequestResponseDto toResponseDto(SkillRequest request, Map<String, String> resourceNameMap) {
        return SkillRequestResponseDto.builder()
                .id(request.getId())
                .resourceId(request.getResourceId())
                .resourceName(resourceNameMap.getOrDefault(request.getResourceId(), request.getResourceId()))
                .categoryName(request.getCategoryName())
                .skillName(request.getSkillName())
                .subskillName(request.getSubskillName())
                .proficiency(request.getProficiency() != null
                        ? request.getProficiency().getProficiencyName() : null)
                .requestStatus(request.getRequestStatus() != null
                        ? request.getRequestStatus().name() : null)
                .requestedAt(request.getRequestedAt())
                .approvedAt(request.getApprovedAt())
                .approvedBy(request.getApprovedBy())
                .remarks(request.getRemarks())
                .build();
    }
}
