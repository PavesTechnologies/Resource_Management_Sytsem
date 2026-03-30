package com.service_imple.availability_service_impl;

import com.repo.allocation_repo.AllocationRepository;
import com.service_interface.ledger_service_interface.AvailabilityCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectTimelineChangeService {

    private final AvailabilityCalculationService calculationService;
    private final AllocationRepository allocationRepository;

    @Transactional
    public void handleProjectTimelineChange(Long projectId, LocalDateTime oldStartDate, LocalDateTime oldEndDate, 
                                           LocalDateTime newStartDate, LocalDateTime newEndDate) {
        try {
            List<com.entity.allocation_entities.ResourceAllocation> allocations = allocationRepository.findByProject_PmsProjectId(projectId);
            
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
            
            if (earliestDate == null || latestDate == null) {
                return;
            }
            
            LocalDate start = earliestDate.toLocalDate();
            LocalDate end = latestDate.toLocalDate();
            
            Set<Long> resourceIds = allocations.stream()
                .map(allocation -> allocation.getResource().getResourceId())
                .collect(Collectors.toSet());
            
            for (Long resourceId : resourceIds) {
                try {
                    calculationService.recalculateForDateRange(resourceId, start, end);
                } catch (Exception e) {
                    log.error("Failed to recalculate availability for resource {} from {} to {}: {}", 
                            resourceId, start, end, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to handle project timeline change for project {}: {}", projectId, e.getMessage());
        }
    }
}
