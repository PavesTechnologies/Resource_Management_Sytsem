package com.service_imple.allocation_service_imple;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity.availability_entities.ResourceAvailabilityLedger;
import com.entity.resource_entities.Resource;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.availability_repo.ResourceAvailabilityLedgerRepository;
import com.repo.resource_repo.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Async service for updating Resource Availability Ledger
 * 
 * This service handles expensive ledger recalculation operations asynchronously
 * to prevent blocking the main allocation API response. The ledger update logic
 * recalculates allocation for each date and is expensive, so it's moved to async processing.
 * 
 * Performance Benefits:
 * - Main allocation API returns immediately after saving allocations
 * - Ledger updates happen in background without affecting user experience
 * - Prevents timeout issues for bulk allocation requests
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityLedgerAsyncService {

    private final AllocationRepository allocationRepository;
    private final ResourceAvailabilityLedgerRepository ledgerRepository;
    private final ResourceRepository resourceRepository;

    /**
     * Async method to update availability ledger for a single allocation
     * This method is executed asynchronously and does not block the main thread
     * 
     * @param allocation The resource allocation that was created/updated
     */
    @Async
    public void updateLedgerAsync(ResourceAllocation allocation) {
        try {
            log.debug("Starting async ledger update for allocation: {}, resource: {}", 
                    allocation.getAllocationId(), allocation.getResource().getResourceId());
            
            Long resourceId = allocation.getResource().getResourceId();
            LocalDate startDate = allocation.getAllocationStartDate();
            LocalDate endDate = allocation.getAllocationEndDate();
            
            // For each day in the allocation period, update the ledger
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                updateLedgerForDate(resourceId, currentDate);
                currentDate = currentDate.plusDays(1);
            }
            
            log.debug("Completed async ledger update for allocation: {}", allocation.getAllocationId());
            
        } catch (Exception e) {
            // Log error but don't throw to avoid affecting async processing
            log.error("Error in async ledger update for allocation {}: {}", 
                    allocation.getAllocationId(), e.getMessage(), e);
        }
    }

    /**
     * Updates the ledger for a specific resource and date by calculating
     * the total allocation percentage from all active allocations.
     * 
     * This method calculates the total allocation percentage for this resource on this date
     * and updates the ledger accordingly.
     * 
     * @param resourceId The resource ID
     * @param date The date to update
     */
    private void updateLedgerForDate(Long resourceId, LocalDate date) {
        try {
            // Calculate total allocation percentage for this resource on this date
            List<ResourceAllocation> activeAllocations = allocationRepository
                .findActiveAllocationsForResourceOnDate(resourceId, date);
            
            int totalAllocation = activeAllocations.stream()
                .mapToInt(ResourceAllocation::getAllocationPercentage)
                .sum();
            
            // Find existing ledger entry using date range (handles monthly ledgers)
            Optional<ResourceAvailabilityLedger> existingLedger = 
                ledgerRepository.findByResourceIdAndDate(resourceId, date);
            
            ResourceAvailabilityLedger ledger;
            if (existingLedger.isPresent()) {
                ledger = existingLedger.get();
                ledger.setTotalAllocation(totalAllocation);
                ledger.setAvailablePercentage(Math.max(0, 100 - totalAllocation));
                ledger.setAvailabilityTrustFlag(true);
                ledger.setLastCalculatedAt(LocalDateTime.now());
            } else {
                // Create new ledger entry
                ledger = new ResourceAvailabilityLedger();
                ledger.setResource(resourceRepository.findById(resourceId).orElse(null));
                ledger.setPeriodStart(date);
                ledger.setPeriodEnd(date); // Daily ledger
                ledger.setTotalAllocation(totalAllocation);
                ledger.setAvailablePercentage(Math.max(0, 100 - totalAllocation));
                ledger.setAvailabilityTrustFlag(true);
                ledger.setLastCalculatedAt(LocalDateTime.now());
            }
            
            ledgerRepository.save(ledger);
            
        } catch (Exception e) {
            // Log error but don't throw to avoid breaking async processing
            log.error("Error updating ledger for resource {} on date {}: {}", 
                    resourceId, date, e.getMessage(), e);
        }
    }
}
