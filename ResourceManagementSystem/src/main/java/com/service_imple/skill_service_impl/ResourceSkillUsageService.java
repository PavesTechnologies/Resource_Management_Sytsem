package com.service_imple.skill_service_impl;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import com.entity.skill_entities.ResourceSkill;
import com.entity.skill_entities.ResourceSubSkill;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.skill_repo.ResourceSkillRepository;
import com.repo.skill_repo.ResourceSubSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain service for managing ResourceSkill and ResourceSubSkill lastUsedDate updates
 * during role-off scenarios.
 *
 * This service ensures that whenever a resource is rolled off from a project,
 * the relevant skills and subskills have their lastUsedDate updated appropriately.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceSkillUsageService {

    private final ResourceSkillRepository resourceSkillRepository;
    private final ResourceSubSkillRepository resourceSubSkillRepository;
    private final AllocationRepository allocationRepository;

    /**
     * Main entry point for updating skill lastUsedDate during role-off.
     * This method determines the appropriate date and updates relevant skills.
     *
     * @param resource The resource being rolled off
     * @param project The project from which resource is being rolled off
     * @param roleOffEffectiveDate The effective role-off date (highest priority)
     * @param allocationEndDate The allocation end date (fallback)
     * @param projectEndDate The project end date (final fallback)
     */
    @Transactional
    public void updateResourceSkillLastUsedOnRoleOff(
            Resource resource,
            Project project,
            LocalDate roleOffEffectiveDate,
            LocalDate allocationEndDate,
            LocalDate projectEndDate) {

        // Determine the date to use for lastUsedDate (priority-based)
        LocalDate effectiveDate = determineEffectiveDate(roleOffEffectiveDate, allocationEndDate, projectEndDate);

        if (effectiveDate == null) {
            log.warn("No effective date found for skill update - resource: {}, project: {}",
                    resource.getResourceId(), project.getPmsProjectId());
            return;
        }

        // Get project-related skills for this resource
        List<UUID> projectSkillIds = getProjectSkillIdsForResource(resource.getResourceId(), project.getPmsProjectId());

        if (projectSkillIds.isEmpty()) {
            log.debug("No project-specific skills found for resource: {} in project: {}",
                    resource.getResourceId(), project.getPmsProjectId());
            return;
        }

        // Update ResourceSkill lastUsedDate for project-specific skills
        updateResourceSkillLastUsedDate(resource.getResourceId(), projectSkillIds, effectiveDate);

        // Update ResourceSubSkill lastUsedDate for project-specific subskills
        updateResourceSubSkillLastUsedDate(resource.getResourceId(), projectSkillIds, effectiveDate);

        log.debug("Updated lastUsedDate to {} for {} skills/subskills of resource {} in project {}",
                effectiveDate, projectSkillIds.size(), resource.getResourceId(), project.getPmsProjectId());
    }

    /**
     * Simplified method for common role-off scenarios where only role-off date is available
     */
    @Transactional
    public void updateResourceSkillLastUsedOnRoleOff(Resource resource, Project project, LocalDate roleOffDate) {
        updateResourceSkillLastUsedOnRoleOff(resource, project, roleOffDate, null, null);
    }

    /**
     * Batch update for multiple role-offs (scheduler use case)
     */
    @Transactional
    public void batchUpdateSkillLastUsedDates(List<RoleOffSkillUpdateRequest> requests) {
        for (RoleOffSkillUpdateRequest request : requests) {
            try {
                updateResourceSkillLastUsedOnRoleOff(
                        request.getResource(),
                        request.getProject(),
                        request.getRoleOffEffectiveDate(),
                        request.getAllocationEndDate(),
                        request.getProjectEndDate()
                );
            } catch (Exception e) {
                log.error("Failed to update skill lastUsedDate for resource: {}, project: {}",
                        request.getResource().getResourceId(), request.getProject().getPmsProjectId(), e);
                // Continue with other requests - don't let one failure break the batch
            }
        }
    }

    /**
     * Determines the effective date based on priority:
     * 1. Role-off effective date (highest priority)
     * 2. Allocation end date
     * 3. Project end date (fallback)
     */
    private LocalDate determineEffectiveDate(LocalDate roleOffDate, LocalDate allocationEndDate, LocalDate projectEndDate) {
        if (roleOffDate != null) {
            return roleOffDate;
        }
        if (allocationEndDate != null) {
            return allocationEndDate;
        }
        if (projectEndDate != null) {
            return projectEndDate;
        }
        return null;
    }

    /**
     * Gets skill IDs that are associated with both the resource and the project.
     * This ensures we only update skills that are relevant to the project.
     */
    private List<UUID> getProjectSkillIdsForResource(Long resourceId, Long projectId) {
        // Find active allocations for this resource in this project
        List<ResourceAllocation> allocations = allocationRepository
                .findAllByProject_PmsProjectIdAndResource_ResourceIdAndAllocationStatus(
                        projectId, resourceId, AllocationStatus.ACTIVE);

        // Also include recently ended allocations (within last 30 days) to catch skills from recent role-offs
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<ResourceAllocation> recentAllocations = allocationRepository
                .findByProject_PmsProjectIdAndResource_ResourceIdAndAllocationStatusAndAllocationEndDateAfter(
                        projectId, resourceId, AllocationStatus.ENDED, thirtyDaysAgo);

        allocations.addAll(recentAllocations);

        // Extract skill IDs from allocations (through demands)
        return allocations.stream()
                .filter(allocation -> allocation.getDemand() != null)
                .filter(allocation -> allocation.getDemand().getRole() != null)
                .map(allocation -> allocation.getDemand().getRole().getSkill())
                .filter(skill -> skill != null)
                .map(skill -> skill.getId())
                .distinct()
                .toList();
    }

    /**
     * Updates ResourceSkill lastUsedDate using batch JPQL for performance
     */
    private void updateResourceSkillLastUsedDate(Long resourceId, List<UUID> skillIds, LocalDate effectiveDate) {
        if (skillIds.isEmpty()) {
            return;
        }

        // Use bulk update for better performance
        int updatedCount = resourceSkillRepository.updateLastUsedDateByResourceIdAndSkillIds(
                resourceId, skillIds, effectiveDate);

        log.debug("Updated lastUsedDate for {} ResourceSkill records", updatedCount);
    }

    /**
     * Updates ResourceSubSkill lastUsedDate using batch JPQL for performance
     */
    private void updateResourceSubSkillLastUsedDate(Long resourceId, List<UUID> skillIds, LocalDate effectiveDate) {
        if (skillIds.isEmpty()) {
            return;
        }

        // Use bulk update for better performance
        int updatedCount = resourceSubSkillRepository.updateLastUsedDateByResourceIdAndSkillIds(
                resourceId, skillIds, effectiveDate);

        log.debug("Updated lastUsedDate for {} ResourceSubSkill records", updatedCount);
    }

    /**
     * DTO for batch role-off skill update requests
     */
    public static class RoleOffSkillUpdateRequest {
        private final Resource resource;
        private final Project project;
        private final LocalDate roleOffEffectiveDate;
        private final LocalDate allocationEndDate;
        private final LocalDate projectEndDate;

        public RoleOffSkillUpdateRequest(Resource resource, Project project,
                                         LocalDate roleOffEffectiveDate, LocalDate allocationEndDate,
                                         LocalDate projectEndDate) {
            this.resource = resource;
            this.project = project;
            this.roleOffEffectiveDate = roleOffEffectiveDate;
            this.allocationEndDate = allocationEndDate;
            this.projectEndDate = projectEndDate;
        }

        public Resource getResource() { return resource; }
        public Project getProject() { return project; }
        public LocalDate getRoleOffEffectiveDate() { return roleOffEffectiveDate; }
        public LocalDate getAllocationEndDate() { return allocationEndDate; }
        public LocalDate getProjectEndDate() { return projectEndDate; }
    }
}
