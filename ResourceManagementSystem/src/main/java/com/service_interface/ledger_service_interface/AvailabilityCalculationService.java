package com.service_interface.ledger_service_interface;

import com.entity.ledger_entities.ResourceAvailabilityLedgerDaily;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AvailabilityCalculationService {
    
    void recalculateForDateRange(Long resourceId, LocalDate startDate, LocalDate endDate);
    
    void recalculateForSingleDate(Long resourceId, LocalDate date);
    
    void markAsUntrustworthy(Long resourceId, LocalDate startDate, LocalDate endDate);
    
    List<ResourceAvailabilityLedgerDaily> getAvailabilityForDateRange(Long resourceId, LocalDate startDate, LocalDate endDate);
    
    Optional<ResourceAvailabilityLedgerDaily> getAvailabilityForDate(Long resourceId, LocalDate date);
    
    void cleanupOldEntries(LocalDate cutoffDate);
    
    Map<String, Object> getAvailabilitySummary(Long resourceId, LocalDate startDate, LocalDate endDate);

    // For LedgerRetryService
    void recalculateDailyWithIdempotency(Long resourceId, LocalDate date, String eventId);
}
