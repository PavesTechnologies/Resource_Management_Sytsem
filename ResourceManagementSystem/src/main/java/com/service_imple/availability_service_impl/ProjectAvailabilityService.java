package com.service_imple.availability_service_impl;

import com.entity.allocation_entities.ResourceAllocation;
import com.repo.allocation_repo.AllocationRepository;
import com.service_interface.availability_service_interface.AvailabilityCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Comprehensive service to handle availability calculations for project-related changes:
 * - Project Creation
 * - Project Updates (timeline changes)
 * - Project Deletion/Archival
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectAvailabilityService {

    private final AvailabilityCalculationService calculationService;
    private final AllocationRepository allocationRepository;

    /**
     * Handle project creation - recalculate availability for newly allocated resources
     */
    @Transactional
    public void handleProjectCreation(Long projectId, LocalDateTime startDate, LocalDateTime endDate) {
        System.out.println("=== PROJECT AVAILABILITY SERVICE: handleProjectCreation called for project " + projectId + " ===");
        log.info("Handling project creation for project ID: {}, timeline: {} to {}", projectId, startDate, endDate);
        
        try {
            List<ResourceAllocation> allocations = allocationRepository.findByProject_PmsProjectId(projectId);
            
            if (allocations.isEmpty()) {
                log.info("No allocations found for new project {}", projectId);
                return;
            }
            
            recalculateAvailabilityForAllocations(allocations, startDate, endDate, "project creation");
            
        } catch (Exception e) {
            log.error("Failed to handle project creation for project {}: {}", projectId, e.getMessage(), e);
        }
    }

    /**
     * Handle project timeline updates - recalculate for affected date ranges
     */
    @Transactional
    public void handleProjectTimelineUpdate(Long projectId, LocalDateTime oldStartDate, LocalDateTime oldEndDate, 
                                          LocalDateTime newStartDate, LocalDateTime newEndDate) {
        System.out.println("=== PROJECT AVAILABILITY SERVICE: handleProjectTimelineUpdate called for project " + projectId + " ===");
        log.info("Handling project timeline update for project ID: {}, old timeline: {} to {}, new timeline: {} to {}", 
                projectId, oldStartDate, oldEndDate, newStartDate, newEndDate);
        
        try {
            List<ResourceAllocation> allocations = allocationRepository.findByProject_PmsProjectId(projectId);
            
            if (allocations.isEmpty()) {
                log.info("No allocations found for project {}", projectId);
                return;
            }
            
            // Determine the complete date range that needs recalculation
            LocalDateTime earliestDate = oldStartDate;
            LocalDateTime latestDate = oldEndDate;
            
            if (newStartDate != null && (earliestDate == null || newStartDate.isBefore(earliestDate))) {
                earliestDate = newStartDate;
            }
            if (newEndDate != null && (latestDate == null || newEndDate.isAfter(latestDate))) {
                latestDate = newEndDate;
            }
            
            recalculateAvailabilityForAllocations(allocations, earliestDate, latestDate, "project timeline update");
            
        } catch (Exception e) {
            log.error("Failed to handle project timeline update for project {}: {}", projectId, e.getMessage(), e);
        }
    }

    /**
     * Handle project deletion/archival - free up resources by recalculating availability
     */
    @Transactional
    public void handleProjectDeletion(Long projectId, LocalDateTime oldStartDate, LocalDateTime oldEndDate) {
        log.info("Handling project deletion for project ID: {}, old timeline: {} to {}", projectId, oldStartDate, oldEndDate);
        
        try {
            List<ResourceAllocation> allocations = allocationRepository.findByProject_PmsProjectId(projectId);
            
            if (allocations.isEmpty()) {
                log.info("No allocations found for deleted project {}", projectId);
                return;
            }
            
            // Recalculate availability for the period when the project was active
            recalculateAvailabilityForAllocations(allocations, oldStartDate, oldEndDate, "project deletion");
            
        } catch (Exception e) {
            log.error("Failed to handle project deletion for project {}: {}", projectId, e.getMessage(), e);
        }
    }

    /**
     * Handle allocation changes for a specific project
     */
    @Transactional
    public void handleAllocationChangeForResource(Long resourceId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Handling allocation change for resource ID: {}, timeline: {} to {}", resourceId, startDate, endDate);
        
        try {
            if (startDate == null || endDate == null) {
                log.warn("Invalid date range for resource allocation recalculation");
                return;
            }
            
            YearMonth startMonth = YearMonth.from(startDate);
            YearMonth endMonth = YearMonth.from(endDate);
            
            YearMonth currentMonth = startMonth;
            while (!currentMonth.isAfter(endMonth)) {
                try {
                    calculationService.recalculateForResource(resourceId, currentMonth);
                    log.debug("Recalculated availability for resource {} in month {}", resourceId, currentMonth);
                } catch (Exception e) {
                    log.error("Failed to recalculate availability for resource {} in month {}: {}", 
                            resourceId, currentMonth, e.getMessage());
                }
                currentMonth = currentMonth.plusMonths(1);
            }
            
        } catch (Exception e) {
            log.error("Failed to handle allocation change for resource {}: {}", resourceId, e.getMessage(), e);
        }
    }

    /**
     * Core method to recalculate availability for affected resources and date ranges
     */
    private void recalculateAvailabilityForAllocations(List<ResourceAllocation> allocations, 
                                                     LocalDateTime startDate, LocalDateTime endDate, 
                                                     String operationType) {
        if (startDate == null || endDate == null) {
            log.warn("Invalid date range for availability recalculation: {} to {}", startDate, endDate);
            return;
        }
        
        // Get unique resource IDs
        Set<Long> resourceIds = allocations.stream()
            .map(allocation -> allocation.getResource().getResourceId())
            .collect(Collectors.toSet());
        
        log.info("Recalculating availability for {} resources due to {}", resourceIds.size(), operationType);
        
        // Calculate affected months
        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);
        
        // Recalculate availability for each affected resource for each affected month
        YearMonth currentMonth = startMonth;
        int totalRecalculations = 0;
        
        while (!currentMonth.isAfter(endMonth)) {
            for (Long resourceId : resourceIds) {
                try {
                    calculationService.recalculateForResource(resourceId, currentMonth);
                    totalRecalculations++;
                    log.debug("Recalculated availability for resource {} in month {}", resourceId, currentMonth);
                } catch (Exception e) {
                    log.error("Failed to recalculate availability for resource {} in month {}: {}", 
                            resourceId, currentMonth, e.getMessage());
                }
            }
            currentMonth = currentMonth.plusMonths(1);
        }
        
        log.info("Completed {} availability recalculations for {}", totalRecalculations, operationType);
    }
}
