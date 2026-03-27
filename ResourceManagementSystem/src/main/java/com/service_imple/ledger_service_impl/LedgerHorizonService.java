package com.service_imple.ledger_service_impl;


import com.service_interface.ledger_service_interface.AllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerHorizonService {

    private final AvailabilityCalculationService availabilityCalculationService;
    private final AllocationService allocationService;
    
    @Value("${ledger.horizon.default-days:90}")
    private int defaultHorizonDays;
    
    @Value("${ledger.horizon.max-days:365}")
    private int maxHorizonDays;
    
    @Value("${ledger.horizon.min-days:30}")
    private int minHorizonDays;

    public LocalDate calculateHorizonEnd(Long resourceId) {
        LocalDate today = LocalDate.now();
        LocalDate defaultHorizon = today.plusDays(defaultHorizonDays);
        
        try {
            LocalDate maxAllocationEnd = getMaxAllocationEndDate(resourceId);
            LocalDate resourceExitDate = getResourceExitDate(resourceId);
            
            LocalDate horizonEnd = defaultHorizon;
            
            if (maxAllocationEnd != null && maxAllocationEnd.isAfter(horizonEnd)) {
                horizonEnd = maxAllocationEnd;
            }
            
            if (resourceExitDate != null && resourceExitDate.isBefore(horizonEnd)) {
                horizonEnd = resourceExitDate;
            }
            
            LocalDate maxAllowed = today.plusDays(maxHorizonDays);
            if (horizonEnd.isAfter(maxAllowed)) {
                horizonEnd = maxAllowed;
            }
            
            log.debug("Calculated horizon end for resource {}: {}", resourceId, horizonEnd);
            return horizonEnd;
            
        } catch (Exception e) {
            log.warn("Error calculating horizon for resource {}, using default: {}", resourceId, e.getMessage());
            return defaultHorizon;
        }
    }

    public LocalDate calculateIncrementalHorizon(Long resourceId, LocalDate baseDate) {
        LocalDate minHorizon = baseDate.plusDays(minHorizonDays);
        LocalDate maxHorizon = baseDate.plusDays(maxHorizonDays);
        
        try {
            LocalDate maxAllocationEnd = getMaxAllocationEndDateAfter(resourceId, baseDate);
            LocalDate resourceExitDate = getResourceExitDate(resourceId);
            
            LocalDate horizonEnd = minHorizon;
            
            if (maxAllocationEnd != null && maxAllocationEnd.isAfter(horizonEnd)) {
                horizonEnd = maxAllocationEnd;
            }
            
            if (resourceExitDate != null && resourceExitDate.isBefore(horizonEnd)) {
                horizonEnd = resourceExitDate;
            }
            
            if (horizonEnd.isAfter(maxHorizon)) {
                horizonEnd = maxHorizon;
            }
            
            return horizonEnd;
            
        } catch (Exception e) {
            log.warn("Error calculating incremental horizon for resource {}, using minimum: {}", resourceId, e.getMessage());
            return minHorizon;
        }
    }

    public boolean isWithinHorizon(Long resourceId, LocalDate date) {
        LocalDate horizonEnd = calculateHorizonEnd(resourceId);
        return !date.isAfter(horizonEnd);
    }

    public LocalDate getOptimalCalculationStartDate(Long resourceId, LocalDate eventDate) {
        LocalDate today = LocalDate.now();
        
        if (eventDate.isBefore(today.minusDays(30))) {
            return today.minusDays(30);
        }
        
        return eventDate.isBefore(today) ? eventDate : today;
    }

    public LocalDate getOptimalCalculationEndDate(Long resourceId, LocalDate eventDate) {
        LocalDate horizonEnd = calculateHorizonEnd(resourceId);
        LocalDate eventHorizon = eventDate.plusDays(defaultHorizonDays);
        
        return eventHorizon.isBefore(horizonEnd) ? eventHorizon : horizonEnd;
    }

    public CompletableFuture<Void> processIncrementalUpdate(Long resourceId, LocalDate eventDate) {
        return CompletableFuture.runAsync(() -> {
            try {
                LocalDate startDate = getOptimalCalculationStartDate(resourceId, eventDate);
                LocalDate endDate = getOptimalCalculationEndDate(resourceId, eventDate);
                
                log.info("Processing incremental update for resource {} from {} to {}", resourceId, startDate, endDate);
                
                availabilityCalculationService.recalculateForDateRange(resourceId, startDate, endDate);
                
            } catch (Exception e) {
                log.error("Failed to process incremental update for resource {}: {}", resourceId, e.getMessage(), e);
                throw new RuntimeException("Incremental update failed", e);
            }
        });
    }

    public CompletableFuture<Void> processBatchIncrementalUpdate(Set<Long> resourceIds, LocalDate eventDate) {
        return CompletableFuture.runAsync(() -> {
            try {
                LocalDate commonStartDate = LocalDate.now().minusDays(30);
                LocalDate commonEndDate = LocalDate.now().plusDays(defaultHorizonDays);
                
                for (Long resourceId : resourceIds) {
                    try {
                        LocalDate resourceStartDate = getOptimalCalculationStartDate(resourceId, eventDate);
                        LocalDate resourceEndDate = getOptimalCalculationEndDate(resourceId, eventDate);
                        
                        LocalDate finalStartDate = resourceStartDate.isAfter(commonStartDate) ? resourceStartDate : commonStartDate;
                        LocalDate finalEndDate = resourceEndDate.isBefore(commonEndDate) ? resourceEndDate : commonEndDate;
                        
                        availabilityCalculationService.recalculateForDateRange(resourceId, finalStartDate, finalEndDate);
                        
                    } catch (Exception e) {
                        log.error("Failed to process incremental update for resource {} in batch: {}", resourceId, e.getMessage(), e);
                    }
                }
                
            } catch (Exception e) {
                log.error("Failed to process batch incremental update: {}", e.getMessage(), e);
                throw new RuntimeException("Batch incremental update failed", e);
            }
        });
    }

    public void optimizeForEvent(Long resourceId, LocalDate eventDate, String eventType) {
        switch (eventType) {
            case "ALLOCATION_CHANGED":
                processAllocationChange(resourceId, eventDate);
                break;
            case "ROLE_OFF":
                processRoleOff(resourceId, eventDate);
                break;
            case "RESOURCE_CREATED":
                processResourceCreation(resourceId, eventDate);
                break;
            default:
                processGenericEvent(resourceId, eventDate);
                break;
        }
    }

    private void processAllocationChange(Long resourceId, LocalDate eventDate) {
        LocalDate startDate = getOptimalCalculationStartDate(resourceId, eventDate);
        LocalDate endDate = getOptimalCalculationEndDate(resourceId, eventDate);
        
        log.info("Processing allocation change for resource {} from {} to {}", resourceId, startDate, endDate);
        availabilityCalculationService.recalculateForDateRange(resourceId, startDate, endDate);
    }

    private void processRoleOff(Long resourceId, LocalDate eventDate) {
        LocalDate startDate = eventDate.isBefore(LocalDate.now()) ? eventDate : LocalDate.now();
        LocalDate endDate = calculateHorizonEnd(resourceId);
        
        log.info("Processing role-off for resource {} from {} to {}", resourceId, startDate, endDate);
        availabilityCalculationService.recalculateForDateRange(resourceId, startDate, endDate);
    }

    private void processResourceCreation(Long resourceId, LocalDate eventDate) {
        LocalDate startDate = eventDate.isBefore(LocalDate.now()) ? eventDate : LocalDate.now();
        LocalDate endDate = calculateHorizonEnd(resourceId);
        
        log.info("Processing resource creation for resource {} from {} to {}", resourceId, startDate, endDate);
        availabilityCalculationService.recalculateForDateRange(resourceId, startDate, endDate);
    }

    private void processGenericEvent(Long resourceId, LocalDate eventDate) {
        LocalDate startDate = getOptimalCalculationStartDate(resourceId, eventDate);
        LocalDate endDate = getOptimalCalculationEndDate(resourceId, eventDate);
        
        log.info("Processing generic event for resource {} from {} to {}", resourceId, startDate, endDate);
        availabilityCalculationService.recalculateForDateRange(resourceId, startDate, endDate);
    }

    private LocalDate getMaxAllocationEndDate(Long resourceId) {
        try {
            return allocationService.getMaxAllocationEndDate(resourceId);
        } catch (Exception e) {
            log.warn("Failed to get max allocation end date for resource {}: {}", resourceId, e.getMessage());
            return null;
        }
    }

    private LocalDate getMaxAllocationEndDateAfter(Long resourceId, LocalDate baseDate) {
        try {
            return allocationService.getMaxAllocationEndDateAfter(resourceId, baseDate);
        } catch (Exception e) {
            log.warn("Failed to get max allocation end date after {} for resource {}: {}", baseDate, resourceId, e.getMessage());
            return null;
        }
    }

    private LocalDate getResourceExitDate(Long resourceId) {
        try {
            return allocationService.getResourceExitDate(resourceId);
        } catch (Exception e) {
            log.warn("Failed to get resource exit date for resource {}: {}", resourceId, e.getMessage());
            return null;
        }
    }

    public HorizonInfo getHorizonInfo(Long resourceId) {
        LocalDate today = LocalDate.now();
        LocalDate horizonEnd = calculateHorizonEnd(resourceId);
        LocalDate maxAllocationEnd = getMaxAllocationEndDate(resourceId);
        LocalDate resourceExitDate = getResourceExitDate(resourceId);
        
        return HorizonInfo.builder()
                .resourceId(resourceId)
                .calculationDate(today)
                .horizonEnd(horizonEnd)
                .horizonDays((int) java.time.temporal.ChronoUnit.DAYS.between(today, horizonEnd))
                .maxAllocationEndDate(maxAllocationEnd)
                .resourceExitDate(resourceExitDate)
                .withinHorizon(maxAllocationEnd != null && !maxAllocationEnd.isAfter(horizonEnd))
                .build();
    }

    public void preloadHorizonsForResources(Set<Long> resourceIds) {
        log.info("Preloading horizon information for {} resources", resourceIds.size());
        
        Set<CompletableFuture<Void>> futures = resourceIds.stream()
                .map(resourceId -> CompletableFuture.runAsync(() -> {
                    try {
                        getHorizonInfo(resourceId);
                    } catch (Exception e) {
                        log.warn("Failed to preload horizon for resource {}: {}", resourceId, e.getMessage());
                    }
                }))
                .collect(Collectors.toSet());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.info("Completed horizon preloading for {} resources", resourceIds.size()))
                .exceptionally(e -> {
                    log.error("Error during horizon preloading: {}", e.getMessage());
                    return null;
                });
    }

    public static class HorizonInfo {
        private final Long resourceId;
        private final LocalDate calculationDate;
        private final LocalDate horizonEnd;
        private final int horizonDays;
        private final LocalDate maxAllocationEndDate;
        private final LocalDate resourceExitDate;
        private final boolean withinHorizon;

        private HorizonInfo(Builder builder) {
            this.resourceId = builder.resourceId;
            this.calculationDate = builder.calculationDate;
            this.horizonEnd = builder.horizonEnd;
            this.horizonDays = builder.horizonDays;
            this.maxAllocationEndDate = builder.maxAllocationEndDate;
            this.resourceExitDate = builder.resourceExitDate;
            this.withinHorizon = builder.withinHorizon;
        }

        public static Builder builder() {
            return new Builder();
        }

        public Long getResourceId() { return resourceId; }
        public LocalDate getCalculationDate() { return calculationDate; }
        public LocalDate getHorizonEnd() { return horizonEnd; }
        public int getHorizonDays() { return horizonDays; }
        public LocalDate getMaxAllocationEndDate() { return maxAllocationEndDate; }
        public LocalDate getResourceExitDate() { return resourceExitDate; }
        public boolean isWithinHorizon() { return withinHorizon; }

        public static class Builder {
            private Long resourceId;
            private LocalDate calculationDate;
            private LocalDate horizonEnd;
            private int horizonDays;
            private LocalDate maxAllocationEndDate;
            private LocalDate resourceExitDate;
            private boolean withinHorizon;

            public Builder resourceId(Long resourceId) { this.resourceId = resourceId; return this; }
            public Builder calculationDate(LocalDate calculationDate) { this.calculationDate = calculationDate; return this; }
            public Builder horizonEnd(LocalDate horizonEnd) { this.horizonEnd = horizonEnd; return this; }
            public Builder horizonDays(int horizonDays) { this.horizonDays = horizonDays; return this; }
            public Builder maxAllocationEndDate(LocalDate maxAllocationEndDate) { this.maxAllocationEndDate = maxAllocationEndDate; return this; }
            public Builder resourceExitDate(LocalDate resourceExitDate) { this.resourceExitDate = resourceExitDate; return this; }
            public Builder withinHorizon(boolean withinHorizon) { this.withinHorizon = withinHorizon; return this; }

            public HorizonInfo build() {
                return new HorizonInfo(this);
            }
        }
    }
}
