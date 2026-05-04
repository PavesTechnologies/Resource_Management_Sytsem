package com.service_imple.availability_service_impl;

import com.entity.allocation_entities.ResourceAllocation;
import com.repo.allocation_repo.AllocationRepository;
import com.service_interface.ledger_service_interface.LedgerAvailabilityCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectAvailabilityService {

    private final LedgerAvailabilityCalculationService calculationService;
    private final AllocationRepository allocationRepository;

    @Transactional
    public void handleProjectCreation(Long projectId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<ResourceAllocation> allocations = allocationRepository.findByProject_PmsProjectId(projectId);
            
            if (allocations.isEmpty()) {
                return;
            }
            
            recalculateAvailabilityForAllocations(allocations, startDate, endDate);
            
        } catch (Exception e) {
            log.error("Failed to handle project creation for project {}: {}", projectId, e.getMessage());
        }
    }

    @Transactional
    public void handleProjectTimelineUpdate(Long projectId, LocalDateTime oldStartDate, LocalDateTime oldEndDate, 
                                          LocalDateTime newStartDate, LocalDateTime newEndDate) {
        try {
            List<ResourceAllocation> allocations = allocationRepository.findByProject_PmsProjectId(projectId);
            
            if (allocations.isEmpty()) {
                return;
            }
            
            LocalDateTime earliestDate = oldStartDate;
            LocalDateTime latestDate = oldEndDate;
            
            if (newStartDate != null && (earliestDate == null || newStartDate.isBefore(earliestDate))) {
                earliestDate = newStartDate;
            }
            if (newEndDate != null && (latestDate == null || newEndDate.isAfter(latestDate))) {
                latestDate = newEndDate;
            }
            
            recalculateAvailabilityForAllocations(allocations, earliestDate, latestDate);
            
        } catch (Exception e) {
            log.error("Failed to handle project timeline update for project {}: {}", projectId, e.getMessage());
        }
    }

    @Transactional
    public void handleProjectDeletion(Long projectId, LocalDateTime oldStartDate, LocalDateTime oldEndDate) {
        try {
            List<ResourceAllocation> allocations = allocationRepository.findByProject_PmsProjectId(projectId);
            
            if (allocations.isEmpty()) {
                return;
            }
            
            recalculateAvailabilityForAllocations(allocations, oldStartDate, oldEndDate);
            
        } catch (Exception e) {
            log.error("Failed to handle project deletion for project {}: {}", projectId, e.getMessage());
        }
    }

    @Transactional
    public void handleAllocationChangeForResource(Long resourceId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            if (startDate == null || endDate == null) {
                return;
            }
            calculationService.recalculateForDateRange(resourceId, startDate.toLocalDate(), endDate.toLocalDate());
        } catch (Exception e) {
            log.error("Failed to handle allocation change for resource {}: {}", resourceId, e.getMessage());
        }
    }

    private void recalculateAvailabilityForAllocations(List<ResourceAllocation> allocations, 
                                                     LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            return;
        }
        
        Set<Long> resourceIds = allocations.stream()
            .map(allocation -> allocation.getResource().getResourceId())
            .collect(Collectors.toSet());
        
        LocalDate start = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();
        
        for (Long resourceId : resourceIds) {
            try {
                calculationService.recalculateForDateRange(resourceId, start, end);
            } catch (Exception e) {
                log.error("Failed to recalculate availability for resource {} from {} to {}: {}", 
                        resourceId, start, end, e.getMessage());
            }
        }
    }
}
