package com.service_imple.availability_service_impl;

import com.entity.resource_entities.Resource;
import com.service_interface.ledger_service_interface.AvailabilityCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceEventService implements com.service_interface.resource_service_interface.ResourceEventService {
    
    private final AvailabilityCalculationService calculationService;
    
    @Async
    public void triggerLedgerCalculationAfterCreate(Long resourceId) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate endOfYear = LocalDate.of(today.getYear(), 12, 31);
            calculationService.recalculateForDateRange(resourceId, today, endOfYear);
        } catch (Exception e) {
            log.error("Async ledger calculation failed after create for resource {}: {}", resourceId, e.getMessage());
        }
    }
    
    @Async
    public void triggerLedgerCalculationAfterUpdate(Long resourceId) {
        try {
            LocalDate today = LocalDate.now();
            calculationService.recalculateForSingleDate(resourceId, today);
        } catch (Exception e) {
            log.error("Async ledger calculation failed after update for resource {}: {}", resourceId, e.getMessage());
        }
    }
    
    @Async
    public void triggerLedgerCleanupAfterDelete(Long resourceId) {
        try {
            calculationService.cleanupOldEntries(LocalDate.now());
        } catch (Exception e) {
            log.error("Async cleanup failed after delete for resource {}: {}", resourceId, e.getMessage());
        }
    }
    
    @Override
    public void publishResourceCreated(Resource resource) {
        triggerLedgerCalculationAfterCreate(resource.getResourceId());
    }
    
    @Override
    public void publishResourceUpdated(Resource resource) {
        triggerLedgerCalculationAfterUpdate(resource.getResourceId());
    }
    
    @Override
    public void publishResourceDeleted(Long resourceId) {
        triggerLedgerCleanupAfterDelete(resourceId);
    }
}
