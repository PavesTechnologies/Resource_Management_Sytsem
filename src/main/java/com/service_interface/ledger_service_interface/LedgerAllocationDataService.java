package com.service_interface.ledger_service_interface;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.YearMonth;

public interface LedgerAllocationDataService {

    @Getter
    @AllArgsConstructor
    class AllocationData {
        private final int confirmedAllocationPercentage;
        private final int draftAllocationPercentage;
    }

    AllocationData getAllocationDataForResourceAndDate(String resourceId, LocalDate date);
    AllocationData getAllocationDataForResourceForMonth(String resourceId, YearMonth yearMonth);

    boolean isApiHealthy();

    LocalDate getMaxAllocationEndDate(String resourceId);

    LocalDate getMaxAllocationEndDateAfter(String resourceId, LocalDate baseDate);

    LocalDate getResourceExitDate(String resourceId);
}
