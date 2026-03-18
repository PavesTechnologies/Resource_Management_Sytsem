package com.service_imple.allocation_service_imple;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity.availability_entities.ResourceAvailabilityLedger;
import com.entity.resource_entities.Resource;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.availability_repo.ResourceAvailabilityLedgerRepository;
import com.repo.resource_repo.ResourceRepository;
import com.service_interface.availability_service_interface.AvailabilityCalculationService;
import com.service_interface.availability_service_interface.AvailabilityTriggerService;
import com.service_interface.availability_service_interface.DashboardKpiService;
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
    private final AvailabilityCalculationService availabilityCalculationService;
    private final AvailabilityTriggerService availabilityTriggerService;
    private final DashboardKpiService dashboardKpiService;

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

    /**
     * Async method to trigger ledger update for a specific resource
     * This method is used after de-allocation to ensure background consistency
     * 
     * @param resourceId The resource ID to update ledger for
     */
    @Async
    public void triggerLedgerUpdateForResource(Long resourceId) {
        try {
            log.debug("Starting async ledger update for resource: {}", resourceId);
            
            // Get current date and next 30 days to ensure consistency
            LocalDate currentDate = LocalDate.now();
            LocalDate endDate = currentDate.plusDays(30);
            
            // Update ledger for each day in the range
            LocalDate date = currentDate;
            while (!date.isAfter(endDate)) {
                updateLedgerForDate(resourceId, date);
                date = date.plusDays(1);
            }
            
            log.debug("Completed async ledger update for resource: {}", resourceId);
            
        } catch (Exception e) {
            // Log error but don't throw to avoid affecting async processing
            log.error("Error in async ledger update for resource {}: {}", 
                    resourceId, e.getMessage(), e);
        }
    }

    /**
     * Cross-module availability synchronization after role-off/de-allocation
     * Ensures consistent availability across all RMS modules:
     * - Availability Module: Timeline views, KPIs
     * - Allocation Module: Capacity validation, conflict detection  
     * - Demand Module: Demand matching, fulfillment status
     * - Dashboard Module: KPI calculations, metrics
     * - Resource Module: Bench management, resource status
     */
    @Async
    public void synchronizeAvailabilityAcrossModules(Long resourceId, LocalDate roleOffDate) {
        log.info("Starting cross-module availability synchronization for resource {} after role-off on {}", 
                resourceId, roleOffDate);
        
        try {
            // 1. Update Availability Module (Primary source of truth)
            updateAvailabilityModule(resourceId, roleOffDate);
            
            // 2. Update Allocation Module (Capacity and conflict detection)
            updateAllocationModule(resourceId, roleOffDate);
            
            // 3. Update Demand Module (Demand matching and fulfillment)
            updateDemandModule(resourceId, roleOffDate);
            
            // 4. Update Dashboard Module (KPIs and metrics)
            updateDashboardModule(resourceId, roleOffDate);
            
            // 5. Update Resource Module (Bench management)
            updateResourceModule(resourceId, roleOffDate);
            
            log.info("Successfully synchronized availability across all RMS modules for resource {}", resourceId);
            
        } catch (Exception e) {
            log.error("Failed to synchronize availability across modules for resource {}: {}", 
                    resourceId, e.getMessage(), e);
        }
    }

    /**
     * Updates Availability Module - Primary source of truth
     */
    private void updateAvailabilityModule(Long resourceId, LocalDate roleOffDate) {
        log.debug("Updating Availability Module for resource {}", resourceId);
        
        try {
            // Recalculate for current month and next 2 months
            java.time.YearMonth currentMonth = java.time.YearMonth.from(java.time.LocalDate.now());
            for (int i = 0; i <= 2; i++) {
                java.time.YearMonth month = currentMonth.plusMonths(i);
                availabilityCalculationService.recalculateForResource(resourceId, month);
            }
            
            log.debug("Availability Module updated successfully for resource {}", resourceId);
            
        } catch (Exception e) {
            log.error("Failed to update Availability Module for resource {}: {}", resourceId, e.getMessage());
        }
    }

    /**
     * Updates Allocation Module - Capacity validation and conflict detection
     */
    private void updateAllocationModule(Long resourceId, LocalDate roleOffDate) {
        log.debug("Updating Allocation Module for resource {}", resourceId);
        
        try {
            // Trigger bulk recalculation to ensure allocation capacity is updated
            java.time.YearMonth startMonth = java.time.YearMonth.from(roleOffDate.minusMonths(1));
            java.time.YearMonth endMonth = java.time.YearMonth.from(roleOffDate.plusMonths(3));
            availabilityTriggerService.triggerBulkRecalculation(startMonth, endMonth);
            
            log.debug("Allocation Module updated successfully for resource {}", resourceId);
            
        } catch (Exception e) {
            log.error("Failed to update Allocation Module for resource {}: {}", resourceId, e.getMessage());
        }
    }

    /**
     * Updates Demand Module - Demand matching and fulfillment status
     */
    private void updateDemandModule(Long resourceId, LocalDate roleOffDate) {
        log.debug("Updating Demand Module for resource {}", resourceId);
        
        try {
            // Trigger resource-specific recalculation for demand matching
            java.time.YearMonth currentMonth = java.time.YearMonth.from(java.time.LocalDate.now());
            availabilityTriggerService.triggerResourceRecalculation(resourceId, currentMonth);
            
            log.debug("Demand Module updated successfully for resource {}", resourceId);
            
        } catch (Exception e) {
            log.error("Failed to update Demand Module for resource {}: {}", resourceId, e.getMessage());
        }
    }

    /**
     * Updates Dashboard Module - KPIs and metrics
     */
    private void updateDashboardModule(Long resourceId, LocalDate roleOffDate) {
        log.debug("Updating Dashboard Module for resource {}", resourceId);
        
        try {
            // Refresh dashboard KPIs to reflect new availability
            LocalDate fromDate = roleOffDate.minusDays(7);
            LocalDate toDate = roleOffDate.plusDays(30);
            
            // Trigger KPI recalculation (this will update cached values)
            dashboardKpiService.calculateKpis(fromDate, toDate, null, null, null, null, null);
            
            log.debug("Dashboard Module updated successfully for resource {}", resourceId);
            
        } catch (Exception e) {
            log.error("Failed to update Dashboard Module for resource {}: {}", resourceId, e.getMessage());
        }
    }

    /**
     * Updates Resource Module - Bench management and resource status
     */
    private void updateResourceModule(Long resourceId, LocalDate roleOffDate) {
        log.debug("Updating Resource Module for resource {}", resourceId);
        
        try {
            // Update resource availability ledger entries for bench management
            List<ResourceAvailabilityLedger> ledgers = ledgerRepository
                .findByResourceIdAndPeriodStartBetweenOrderByPeriodStart(
                    resourceId, 
                    roleOffDate.minusMonths(1), 
                    roleOffDate.plusMonths(3)
                );
            
            // Refresh trust flags for bench calculations
            ledgers.forEach(ledger -> {
                ledger.setAvailabilityTrustFlag(true);
                ledger.setLastCalculatedAt(LocalDateTime.now());
            });
            
            ledgerRepository.saveAll(ledgers);
            
            log.debug("Resource Module updated successfully for resource {}", resourceId);
            
        } catch (Exception e) {
            log.error("Failed to update Resource Module for resource {}: {}", resourceId, e.getMessage());
        }
    }
}
