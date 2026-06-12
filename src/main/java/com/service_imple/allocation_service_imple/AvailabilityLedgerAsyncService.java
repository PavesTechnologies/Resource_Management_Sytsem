package com.service_imple.allocation_service_imple;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity.ledger_entities.DeadLetterQueue;
import com.entity_enums.ledger_enums.DLQStatus;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.ledger_repo.DeadLetterQueueRepository;
import com.service_interface.ledger_service_interface.LedgerAvailabilityCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityLedgerAsyncService {

    private final AllocationRepository allocationRepository;
    private final LedgerAvailabilityCalculationService availabilityCalculationService;
    private final DeadLetterQueueRepository deadLetterQueueRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRIES = 3;

    @Async
    public void updateLedgerAsync(ResourceAllocation allocation) {
        String resourceId = allocation.getResource().getResourceId();
        LocalDate startDate = allocation.getAllocationStartDate();
        LocalDate endDate = allocation.getAllocationEndDate();
        recalculateWithRetry("ALLOCATION_UPDATE", resourceId, startDate, endDate);
    }

    @Async
    public void updateLedger(String resourceId, LocalDate startDate, LocalDate endDate) {
        recalculateWithRetry("RANGE_UPDATE", resourceId, startDate, endDate);
    }

    @Async
    public void triggerLedgerUpdateForResource(String resourceId) {
        LocalDate currentDate = LocalDate.now();
        LocalDate endDate = calculateHorizonEnd(resourceId, currentDate);
        recalculateWithRetry("RESOURCE_UPDATE", resourceId, currentDate, endDate);
    }

    @Async
    public void synchronizeAvailabilityAcrossModules(String resourceId, LocalDate roleOffDate) {
        LocalDate currentDate = LocalDate.now();
        LocalDate endDate = calculateHorizonEnd(resourceId, currentDate);
        recalculateWithRetry("SYNC_UPDATE", resourceId, roleOffDate, endDate);
    }

    private void recalculateWithRetry(String eventType, String resourceId, LocalDate startDate, LocalDate endDate) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                availabilityCalculationService.recalculateForDateRange(resourceId, startDate, endDate);
                return;
            } catch (OptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.error("Ledger calculation failed after {} retries for resource {}, saving to DLQ", MAX_RETRIES, resourceId, e);
                    saveToDeadLetterQueue(eventType, resourceId, startDate, endDate, e);
                    return;
                }
                log.warn("Optimistic lock conflict for resource {} (attempt {}/{}), retrying...", resourceId, attempt, MAX_RETRIES);
                try {
                    Thread.sleep(100L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } catch (Exception e) {
                log.error("Ledger calculation failed for resource {}, saving to DLQ", resourceId, e);
                saveToDeadLetterQueue(eventType, resourceId, startDate, endDate, e);
                return;
            }
        }
    }

    private LocalDate calculateHorizonEnd(String resourceId, LocalDate currentDate) {
        LocalDate maxAllocationEnd = allocationRepository
                .findMaxAllocationEndDateForResource(resourceId)
                .orElse(currentDate.plusMonths(3));

        LocalDate horizonEnd = currentDate.plusDays(90);
        if (maxAllocationEnd.isAfter(horizonEnd)) {
            horizonEnd = maxAllocationEnd;
        }

        return horizonEnd;
    }

    private void saveToDeadLetterQueue(String eventType, String resourceId,
                                       LocalDate startDate, LocalDate endDate, Exception exception) {
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
