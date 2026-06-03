package com.service_interface.ledger_service_interface;

import com.entity.ledger_entities.ResourceAvailabilityLedgerDaily;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LedgerAvailabilityCalculationService {
    
    void recalculateForDateRange(String resourceId, LocalDate startDate, LocalDate endDate);
    
    void recalculateForSingleDate(String resourceId, LocalDate date);
    
    void markAsUntrustworthy(String resourceId, LocalDate startDate, LocalDate endDate);
    
    List<ResourceAvailabilityLedgerDaily> getAvailabilityForDateRange(String resourceId, LocalDate startDate, LocalDate endDate);
    
    Optional<ResourceAvailabilityLedgerDaily> getAvailabilityForDate(String resourceId, LocalDate date);
    
    void cleanupOldEntries(LocalDate cutoffDate);
    
    Map<String, Object> getAvailabilitySummary(String resourceId, LocalDate startDate, LocalDate endDate);

    // For LedgerRetryService
    void recalculateDailyWithIdempotency(String resourceId, LocalDate date, String eventId);
}
