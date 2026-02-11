package com.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.YearMonth;

@Service
public class ResourceEventService {
    
    private final com.service_interface.availability_interface.AvailabilityCalculationService availabilityCalculationService;
    
    public ResourceEventService(com.service_interface.availability_interface.AvailabilityCalculationService availabilityCalculationService) {
        this.availabilityCalculationService = availabilityCalculationService;
    }
    
    @Async
    public void triggerLedgerCalculationAfterCreate(Long resourceId) {
        try {
            // Calculate from current month until end of current year
            YearMonth currentMonth = YearMonth.now();
            YearMonth endOfYear = YearMonth.of(currentMonth.getYear(), 12);
            
            YearMonth month = currentMonth;
            while (!month.isAfter(endOfYear)) {
                try {
                    availabilityCalculationService.recalculateForResource(resourceId, month);
                    month = month.plusMonths(1);
                } catch (Exception e) {
                    // Stop ledger calculation if external API fails
                    System.err.println("External API failed during ledger calculation for resource " + 
                                     resourceId + " in month " + month + ". Stopping calculation. Error: " + e.getMessage());
                    return; // Stop processing further months
                }
            }
        } catch (Exception e) {
            System.err.println("Error in async ledger calculation after create: " + e.getMessage());
        }
    }
    
    @Async
    public void triggerLedgerCalculationAfterUpdate(Long resourceId) {
        try {
            // Recalculate only current month for updated resources
            YearMonth currentMonth = YearMonth.now();
            availabilityCalculationService.recalculateForResource(resourceId, currentMonth);
        } catch (Exception e) {
            // Stop if external API fails
            System.err.println("External API failed during ledger calculation for updated resource " + 
                             resourceId + " in month " + YearMonth.now() + ". Error: " + e.getMessage());
        }
    }
    
    @Async
    public void triggerLedgerCleanupAfterDelete(Long resourceId) {
        try {
            // This will be handled by the repository cleanup
            System.out.println("Async cleanup triggered for resource: " + resourceId);
        } catch (Exception e) {
            System.err.println("Error in async cleanup after delete: " + e.getMessage());
        }
    }
}
