package com.service_interface.ledger_service_interface;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.YearMonth;

public interface AllocationService {
    
    @Getter
    @AllArgsConstructor
    class AllocationData {
        private final int confirmedAllocationPercentage;
        private final int draftAllocationPercentage;
    }
    
    // Cached wrapper methods (internal use)
    AllocationData getAllocationDataCached(Long resourceId, LocalDate date);
    AllocationData getAllocationDataCachedForMonth(Long resourceId, YearMonth yearMonth);
    
    // Safe public methods (used by ledger calculation)
    AllocationData getAllocationDataForResourceAndDate(Long resourceId, LocalDate date);
    AllocationData getAllocationDataForResourceForMonth(Long resourceId, YearMonth yearMonth);
    
    boolean isApiHealthy();
    
    LocalDate getMaxAllocationEndDate(Long resourceId);
    
    LocalDate getMaxAllocationEndDateAfter(Long resourceId, LocalDate baseDate);
    
    LocalDate getResourceExitDate(Long resourceId);
}
