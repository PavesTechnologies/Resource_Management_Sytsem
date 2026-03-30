package com.service_imple.allocation_service_imple;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity.ledger_entities.DeadLetterQueue;
import com.entity_enums.ledger_enums.DLQStatus;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.ledger_repo.DeadLetterQueueRepository;
import com.service_interface.ledger_service_interface.AvailabilityCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityLedgerAsyncServiceRefactored {

    private final AllocationRepository allocationRepository;
    private final AvailabilityCalculationService availabilityCalculationService;
    private final DeadLetterQueueRepository deadLetterQueueRepository;
    private final ObjectMapper objectMapper;

    @Async
    public void updateLedgerAsync(ResourceAllocation allocation) {
        try {
            Long resourceId = allocation.getResource().getResourceId();
            LocalDate startDate = allocation.getAllocationStartDate();
            LocalDate endDate = allocation.getAllocationEndDate();
            
            availabilityCalculationService.recalculateForDateRange(resourceId, startDate, endDate);
        } catch (Exception e) {
            log.error("Ledger calculation failed for allocation {}, saving to DLQ", 
                    allocation.getAllocationId(), e);
            
            saveToDeadLetterQueue("ALLOCATION_UPDATE", allocation.getResource().getResourceId(), 
                    allocation.getAllocationStartDate(), allocation.getAllocationEndDate(), e);
        }
    }

    @Async
    public void updateLedger(Long resourceId, LocalDate startDate, LocalDate endDate) {
        try {
            availabilityCalculationService.recalculateForDateRange(resourceId, startDate, endDate);
        } catch (Exception e) {
            log.error("Ledger calculation failed for resource {}, saving to DLQ", resourceId, e);
            
            saveToDeadLetterQueue("RANGE_UPDATE", resourceId, startDate, endDate, e);
        }
    }

    @Async
    public void triggerLedgerUpdateForResource(Long resourceId) {
        LocalDate currentDate = LocalDate.now();
        LocalDate endDate = calculateHorizonEnd(resourceId, currentDate);
        try {
            availabilityCalculationService.recalculateForDateRange(resourceId, currentDate, endDate);
        } catch (Exception e) {
            log.error("Ledger calculation failed for resource {}, saving to DLQ", resourceId, e);
            
            saveToDeadLetterQueue("RESOURCE_UPDATE", resourceId, currentDate, endDate, e);
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
        LocalDate currentDate = LocalDate.now();
        LocalDate endDate = calculateHorizonEnd(resourceId, currentDate);
        try {
            availabilityCalculationService.recalculateForDateRange(resourceId, roleOffDate, endDate);
        } catch (Exception e) {
            log.error("Ledger calculation failed for resource {}, saving to DLQ", resourceId, e);
            
            saveToDeadLetterQueue("SYNC_UPDATE", resourceId, roleOffDate, endDate, e);
        }
    }

    private void saveToDeadLetterQueue(String eventType, Long resourceId, LocalDate startDate, LocalDate endDate, Exception exception) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("resourceId", resourceId);
            payload.put("startDate", startDate);
            payload.put("endDate", endDate);
            payload.put("eventType", eventType);

            DeadLetterQueue dlq = DeadLetterQueue.builder()
                    .eventId(eventType + "_" + resourceId + "_" + System.currentTimeMillis())
                    .payload(objectMapper.writeValueAsString(payload))
                    .errorMessage(exception.getMessage())
                    .retryCount(0)
                    .maxRetryCount(5)
                    .status(DLQStatus.PENDING_RETRY)
                    .nextRetryAt(java.time.LocalDateTime.now().plusMinutes(5))
                    .originalEventType(eventType)
                    .resourceId(resourceId)
                    .eventDate(java.time.LocalDateTime.now())
                    .build();

            deadLetterQueueRepository.save(dlq);
        } catch (Exception dlqEx) {
            log.error("Failed to save to DLQ for resource {}: {}", resourceId, dlqEx.getMessage());
        }
    }
}
