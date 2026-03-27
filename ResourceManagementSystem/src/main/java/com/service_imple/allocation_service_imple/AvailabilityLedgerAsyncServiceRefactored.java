package com.service_imple.allocation_service_imple;

import com.entity.allocation_entities.ResourceAllocation;
import com.repo.allocation_repo.AllocationRepository;
import com.service_interface.ledger_service_interface.AvailabilityCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityLedgerAsyncServiceRefactored {

    private final AllocationRepository allocationRepository;
    private final AvailabilityCalculationService availabilityCalculationService;

    @Async
    public void updateLedgerAsync(ResourceAllocation allocation) {
        try {
            String eventId = generateEventId("ALLOCATION_CHANGED", allocation);
            Long resourceId = allocation.getResource().getResourceId();
            LocalDate startDate = allocation.getAllocationStartDate();
            LocalDate endDate = allocation.getAllocationEndDate();
            
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                String dateEventId = eventId + "_" + currentDate;
                availabilityCalculationService.recalculateDailyWithIdempotency(resourceId, currentDate, dateEventId);
                currentDate = currentDate.plusDays(1);
            }
        } catch (Exception e) {
            log.error("Error in async ledger update for allocation {}: {}", 
                    allocation.getAllocationId(), e.getMessage());
        }
    }

    @Async
    public void updateLedger(Long resourceId, LocalDate startDate, LocalDate endDate) {
        try {
            String eventId = generateEventId("RANGE_UPDATE", resourceId, startDate, endDate);
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                String dateEventId = eventId + "_" + currentDate;
                availabilityCalculationService.recalculateDailyWithIdempotency(resourceId, currentDate, dateEventId);
                currentDate = currentDate.plusDays(1);
            }
        } catch (Exception e) {
            log.error("Error in async ledger update for resource {}: {}", resourceId, e.getMessage());
        }
    }

    private String generateEventId(String eventType, Object... params) {
        StringBuilder sb = new StringBuilder(eventType);
        for (Object param : params) {
            sb.append("_").append(param.toString());
        }
        sb.append("_").append(System.currentTimeMillis());
        return sb.toString();
    }

    @Async
    public void triggerLedgerUpdateForResource(Long resourceId) {
        try {
            LocalDate currentDate = LocalDate.now();
            LocalDate endDate = calculateHorizonEnd(resourceId, currentDate);
            availabilityCalculationService.recalculateForDateRange(resourceId, currentDate, endDate);
        } catch (Exception e) {
            log.error("Error in async ledger update for resource {}: {}", resourceId, e.getMessage());
        }
    }

    private LocalDate calculateHorizonEnd(Long resourceId, LocalDate currentDate) {
        LocalDate maxAllocationEnd = allocationRepository
                .findMaxAllocationEndDateForResource(resourceId)
                .orElse(currentDate.plusMonths(3));
        
        LocalDate horizonEnd = currentDate.plusDays(90);
        if (maxAllocationEnd.isAfter(horizonEnd)) {
            horizonEnd = maxAllocationEnd;
        }
        
        return horizonEnd;
    }

    @Async
    public void synchronizeAvailabilityAcrossModules(Long resourceId, LocalDate roleOffDate) {
        try {
            LocalDate currentDate = LocalDate.now();
            LocalDate endDate = calculateHorizonEnd(resourceId, currentDate);
            availabilityCalculationService.recalculateForDateRange(resourceId, roleOffDate, endDate);
        } catch (Exception e) {
            log.error("Failed to synchronize availability across modules for resource {}: {}", 
                    resourceId, e.getMessage());
        }
    }
}
