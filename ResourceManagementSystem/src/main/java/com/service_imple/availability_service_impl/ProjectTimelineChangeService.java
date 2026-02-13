package com.service_imple.availability_service_impl;

import com.repo.allocation_repo.AllocationRepository;
import com.service_interface.availability_service_interface.AvailabilityCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectTimelineChangeService {

    private final AvailabilityCalculationService calculationService;
    private final AllocationRepository allocationRepository;

    @Transactional
    public void handleProjectTimelineChange(Long projectId, LocalDateTime oldStartDate, LocalDateTime oldEndDate, 
                                           LocalDateTime newStartDate, LocalDateTime newEndDate) {
        try {
            // Find all resources allocated to this project
            List<com.entity.allocation_entities.ResourceAllocation> allocations = allocationRepository.findByProject_PmsProjectId(projectId);
            
            if (allocations.isEmpty()) {
                return; // No resources affected
            }
            
            // Determine the date range that needs recalculation
            LocalDateTime earliestDate = oldStartDate;
            LocalDateTime latestDate = oldEndDate;
            
            if (newStartDate != null && (earliestDate == null || newStartDate.isBefore(earliestDate))) {
                earliestDate = newStartDate;
            }
            if (newEndDate != null && (latestDate == null || newEndDate.isAfter(latestDate))) {
                latestDate = newEndDate;
            }
            
            if (earliestDate == null || latestDate == null) {
                return; // Insufficient data for recalculation
            }
            
            // Calculate affected months
            YearMonth startMonth = YearMonth.from(earliestDate);
            YearMonth endMonth = YearMonth.from(latestDate);
            
            // Get unique resource IDs
            Set<Long> resourceIds = allocations.stream()
                .map(allocation -> allocation.getResource().getResourceId())
                .collect(Collectors.toSet());
            
            // Recalculate availability for each affected resource for each affected month
            YearMonth currentMonth = startMonth;
            while (!currentMonth.isAfter(endMonth)) {
                for (Long resourceId : resourceIds) {
                    try {
                        calculationService.recalculateForResource(resourceId, currentMonth);
                    } catch (Exception e) {
                        // Continue with other resources/months even if one fails
                        System.err.println("Failed to recalculate availability for resource " + resourceId + 
                                         " in month " + currentMonth + ": " + e.getMessage());
                    }
                }
                currentMonth = currentMonth.plusMonths(1);
            }
            
        } catch (Exception e) {
            // Log error but don't fail the entire CDC processing
            System.err.println("Failed to handle project timeline change for project " + projectId + ": " + e.getMessage());
        }
    }
}
