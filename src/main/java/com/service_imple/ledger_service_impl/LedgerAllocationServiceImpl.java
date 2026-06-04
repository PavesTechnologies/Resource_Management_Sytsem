package com.service_imple.ledger_service_impl;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.resource_repo.ResourceRepository;
import com.service_interface.ledger_service_interface.LedgerAllocationDataService;
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
    private final ResourceRepository resourceRepository;

    @Override
    public AllocationData getAllocationDataForResourceAndDate(String resourceId, LocalDate date) {
        try {
            List<ResourceAllocation> allocations =
                    allocationRepository.findByResource_ResourceIdAndAllocationStartDateLessThanEqualAndAllocationEndDateGreaterThanEqual(
                            resourceId, date);
            return sumAllocations(allocations);
        } catch (Exception e) {
            log.error("Failed to fetch allocation for resource {} on date {}: {}", resourceId, date, e.getMessage());
            return new AllocationData(0, 0);
        }
    }

    @Override
    public AllocationData getAllocationDataForResourceForMonth(String resourceId, YearMonth yearMonth) {
        try {
            LocalDate monthStart = yearMonth.atDay(1);
            LocalDate monthEnd = yearMonth.atEndOfMonth();
            List<ResourceAllocation> allocations =
                    allocationRepository.findConflictingAllocations(resourceId, monthStart, monthEnd);
            return sumAllocations(allocations);
        } catch (Exception e) {
            log.error("Failed to fetch allocation for resource {} for month {}: {}", resourceId, yearMonth, e.getMessage());
            return new AllocationData(0, 0);
        }
    }

    @Override
    public LocalDate getMaxAllocationEndDate(String resourceId) {
        try {
            return allocationRepository.findMaxAllocationEndDateForResource(resourceId).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get max allocation end date for resource {}: {}", resourceId, e.getMessage());
            return null;
        }
    }

    @Override
    public LocalDate getMaxAllocationEndDateAfter(String resourceId, LocalDate baseDate) {
        try {
            return allocationRepository.findMaxAllocationEndDateAfter(resourceId, baseDate).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get max end date after {} for resource {}: {}", baseDate, resourceId, e.getMessage());
            return null;
        }
    }

    @Override
    public LocalDate getResourceExitDate(String resourceId) {
        try {
            return resourceRepository.findById(resourceId)
                    .map(r -> r.getDateOfExit())
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get exit date for resource {}: {}", resourceId, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isApiHealthy() {
        return true;
    }

    public CompletableFuture<AllocationData> getAllocationDataForResourceAndDateAsync(String resourceId, LocalDate date) {
        return CompletableFuture.supplyAsync(() -> getAllocationDataForResourceAndDate(resourceId, date));
    }

    private AllocationData sumAllocations(List<ResourceAllocation> allocations) {
        int confirmedPercentage = 0;
        int draftPercentage = 0;
        for (ResourceAllocation allocation : allocations) {
            if (allocation.getAllocationStatus() == AllocationStatus.ACTIVE) {
                confirmedPercentage += allocation.getAllocationPercentage();
            } else if (allocation.getAllocationStatus() == AllocationStatus.PLANNED) {
                draftPercentage += allocation.getAllocationPercentage();
            }
        }
        return new AllocationData(confirmedPercentage, draftPercentage);
    }
}
