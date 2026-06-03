package com.service_imple.ledger_service_impl;

import com.service_interface.ledger_service_interface.LedgerAllocationDataService;
import com.repo.allocation_repo.AllocationRepository;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity_enums.allocation_enums.AllocationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service("ledgerAllocationService")
@RequiredArgsConstructor
@Slf4j
public class LedgerAllocationServiceImpl implements LedgerAllocationDataService {

    private final AllocationRepository allocationRepository;

    @Override
    public AllocationData getAllocationDataForResourceAndDate(String resourceId, LocalDate date) {
        return getAllocationDataInternal(resourceId, date);
    }

    private AllocationData getAllocationDataInternal(String resourceId, LocalDate date) {
        try {
            List<ResourceAllocation> allocations = allocationRepository.findActiveAllocationsForResourceOnDate(resourceId, date);

            int confirmedPercentage = 0;
            int draftPercentage = 0;

            for (ResourceAllocation allocation : allocations) {
                if (allocation.getAllocationStatus() == AllocationStatus.ACTIVE) {
                    confirmedPercentage += allocation.getAllocationPercentage();
                } else if (allocation.getAllocationStatus() == AllocationStatus.PLANNED) { // Assuming PLANNED status represents draft
                    draftPercentage += allocation.getAllocationPercentage();
                }
            }

            return new AllocationData(confirmedPercentage, draftPercentage);

        } catch (Exception e) {
            log.error("Error fetching allocation for resource {} on date {}: {}", resourceId, date, e.getMessage());
            return new AllocationData(0, 0);
        }
    }

    @Override
    public AllocationData getAllocationDataForResourceForMonth(String resourceId, YearMonth yearMonth) {
        return getAllocationDataInternalForMonth(resourceId, yearMonth);
    }

    private AllocationData getAllocationDataInternalForMonth(String resourceId, YearMonth yearMonth) {
        int totalConfirmedPercentage = 0;
        int totalDraftPercentage = 0;
        int daysInMonth = yearMonth.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDate = yearMonth.atDay(day);
            AllocationData dailyAllocation = getAllocationDataInternal(resourceId, currentDate);
            totalConfirmedPercentage += dailyAllocation.getConfirmedAllocationPercentage();
            totalDraftPercentage += dailyAllocation.getDraftAllocationPercentage();
        }

        // Calculate average percentages for the month
        int avgConfirmedPercentage = daysInMonth > 0 ? totalConfirmedPercentage / daysInMonth : 0;
        int avgDraftPercentage = daysInMonth > 0 ? totalDraftPercentage / daysInMonth : 0;

        return new AllocationData(avgConfirmedPercentage, avgDraftPercentage);
    }

    public CompletableFuture<AllocationData> getAllocationDataForResourceAndDateAsync(String resourceId, LocalDate date) {
        return CompletableFuture.supplyAsync(() -> getAllocationDataForResourceAndDate(resourceId, date));
    }


}
