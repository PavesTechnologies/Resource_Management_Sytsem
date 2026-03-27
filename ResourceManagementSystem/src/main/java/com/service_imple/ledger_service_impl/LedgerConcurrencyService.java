package com.service_imple.ledger_service_impl;

import com.entity.ledger_entities.ResourceAvailabilityLedgerDaily;
import com.repo.ledger_repo.ResourceAvailabilityLedgerDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerConcurrencyService {

    private final ResourceAvailabilityLedgerDailyRepository ledgerRepository;
    private final DistributedLockService distributedLockService;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 100;

    @Retryable(
        retryFor = {OptimisticLockingFailureException.class},
        maxAttempts = MAX_RETRY_ATTEMPTS,
        backoff = @Backoff(delay = RETRY_DELAY_MS, multiplier = 2)
    )
    @Transactional
    public ResourceAvailabilityLedgerDaily updateLedgerWithOptimisticLock(
            ResourceAvailabilityLedgerDaily ledgerEntry, String eventId) {
        
        try {
            Optional<ResourceAvailabilityLedgerDaily> existing = ledgerRepository
                    .findByResourceIdAndDate(ledgerEntry.getResourceId(), ledgerEntry.getDate());

            if (existing.isPresent()) {
                ResourceAvailabilityLedgerDaily existingEntry = existing.get();
                
                if (shouldUpdateExistingEntry(existingEntry, ledgerEntry, eventId)) {
                    updateExistingEntry(existingEntry, ledgerEntry, eventId);
                    return ledgerRepository.save(existingEntry);
                } else {
                    return existingEntry;
                }
            } else {
                ledgerEntry.setLastEventId(eventId);
                return ledgerRepository.save(ledgerEntry);
            }

        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic lock conflict for resource {} on date {}, retrying...", 
                    ledgerEntry.getResourceId(), ledgerEntry.getDate());
            throw e;
        } catch (Exception e) {
            log.error("Failed to update ledger entry for resource {} on date {}: {}", 
                    ledgerEntry.getResourceId(), ledgerEntry.getDate(), e.getMessage());
            throw new RuntimeException("Failed to update ledger entry", e);
        }
    }

    @Retryable(
        retryFor = {OptimisticLockingFailureException.class},
        maxAttempts = MAX_RETRY_ATTEMPTS,
        backoff = @Backoff(delay = RETRY_DELAY_MS, multiplier = 2)
    )
    @Transactional
    public ResourceAvailabilityLedgerDaily updateLedgerWithVersionCheck(
            Long resourceId, 
            java.time.LocalDate date, 
            Long expectedVersion,
            ResourceAvailabilityLedgerDaily newValues,
            String eventId) {
        
        String lockKey = "ledger-update-" + resourceId + "-" + date;
        
        return distributedLockService.executeWithLock(lockKey, () -> {
            Optional<ResourceAvailabilityLedgerDaily> existing = ledgerRepository
                    .findByResourceIdAndDate(resourceId, date);

            if (existing.isPresent()) {
                ResourceAvailabilityLedgerDaily existingEntry = existing.get();
                
                if (!existingEntry.getVersion().equals(expectedVersion)) {
                    throw new OptimisticLockingFailureException("Version mismatch detected");
                }

                updateExistingEntry(existingEntry, newValues, eventId);
                return ledgerRepository.save(existingEntry);
            } else {
                newValues.setLastEventId(eventId);
                return ledgerRepository.save(newValues);
            }
        });
    }

    @Transactional
    public boolean updateLedgerWithConditionalLogic(
            Long resourceId, 
            java.time.LocalDate date, 
            String eventId,
            LedgerUpdateCondition condition) {
        
        String lockKey = "ledger-conditional-" + resourceId + "-" + date;
        
        return distributedLockService.executeWithLock(lockKey, () -> {
            Optional<ResourceAvailabilityLedgerDaily> existing = ledgerRepository
                    .findByResourceIdAndDate(resourceId, date);

            if (existing.isPresent()) {
                ResourceAvailabilityLedgerDaily existingEntry = existing.get();
                
                if (condition.shouldUpdate(existingEntry)) {
                    condition.applyUpdate(existingEntry, eventId);
                    ledgerRepository.save(existingEntry);
                    return true;
                }
            }
            return false;
        });
    }

    private boolean shouldUpdateExistingEntry(
            ResourceAvailabilityLedgerDaily existing, 
            ResourceAvailabilityLedgerDaily newEntry, 
            String eventId) {
        
        if (eventId != null && eventId.equals(existing.getLastEventId())) {
            return false;
        }

        if (newEntry.getCalculationVersion() <= existing.getCalculationVersion()) {
            return false;
        }

        return hasSignificantChanges(existing, newEntry);
    }

    private boolean hasSignificantChanges(
            ResourceAvailabilityLedgerDaily existing, 
            ResourceAvailabilityLedgerDaily newEntry) {
        
        return !existing.getStandardHours().equals(newEntry.getStandardHours()) ||
               !existing.getHolidayHours().equals(newEntry.getHolidayHours()) ||
               !existing.getLeaveHours().equals(newEntry.getLeaveHours()) ||
               !existing.getConfirmedAllocHours().equals(newEntry.getConfirmedAllocHours()) ||
               !existing.getDraftAllocHours().equals(newEntry.getDraftAllocHours()) ||
               !existing.getTotalAllocationPercentage().equals(newEntry.getTotalAllocationPercentage()) ||
               !existing.getAvailablePercentage().equals(newEntry.getAvailablePercentage()) ||
               !existing.getIsOverallocated().equals(newEntry.getIsOverallocated()) ||
               !existing.getAvailabilityTrustFlag().equals(newEntry.getAvailabilityTrustFlag());
    }

    private void updateExistingEntry(
            ResourceAvailabilityLedgerDaily existing, 
            ResourceAvailabilityLedgerDaily newValues, 
            String eventId) {
        
        existing.setStandardHours(newValues.getStandardHours());
        existing.setHolidayHours(newValues.getHolidayHours());
        existing.setLeaveHours(newValues.getLeaveHours());
        existing.setConfirmedAllocHours(newValues.getConfirmedAllocHours());
        existing.setDraftAllocHours(newValues.getDraftAllocHours());
        existing.setTotalAllocationPercentage(newValues.getTotalAllocationPercentage());
        existing.setAvailablePercentage(newValues.getAvailablePercentage());
        existing.setIsOverallocated(newValues.getIsOverallocated());
        existing.setOverAllocationPercentage(newValues.getOverAllocationPercentage());
        existing.setAvailabilityTrustFlag(newValues.getAvailabilityTrustFlag());
        existing.setCalculationVersion(newValues.getCalculationVersion());
        existing.setLastEventId(eventId);
        existing.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Optional<ResourceAvailabilityLedgerDaily> getLedgerEntryForUpdate(Long resourceId, java.time.LocalDate date) {
        return ledgerRepository.findByResourceIdAndDate(resourceId, date);
    }

    @Transactional
    public void markLedgerEntriesAsStale(Long resourceId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        String lockKey = "ledger-stale-" + resourceId + "-" + startDate + "-" + endDate;
        
        distributedLockService.executeWithLockVoid(lockKey, () -> {
            List<ResourceAvailabilityLedgerDaily> entries = ledgerRepository
                    .findByResourceIdAndDateBetween(resourceId, startDate, endDate);
            
            for (ResourceAvailabilityLedgerDaily entry : entries) {
                entry.setAvailabilityTrustFlag(false);
                entry.setUpdatedAt(LocalDateTime.now());
            }
            
            if (!entries.isEmpty()) {
                ledgerRepository.saveAll(entries);
            }
        });
    }

    @FunctionalInterface
    public interface LedgerUpdateCondition {
        boolean shouldUpdate(ResourceAvailabilityLedgerDaily existingEntry);
        
        default void applyUpdate(ResourceAvailabilityLedgerDaily existingEntry, String eventId) {
            existingEntry.setLastEventId(eventId);
            existingEntry.setUpdatedAt(LocalDateTime.now());
        }
    }

    public static class TrustFlagCondition implements LedgerUpdateCondition {
        private final boolean expectedTrustFlag;

        public TrustFlagCondition(boolean expectedTrustFlag) {
            this.expectedTrustFlag = expectedTrustFlag;
        }

        @Override
        public boolean shouldUpdate(ResourceAvailabilityLedgerDaily existingEntry) {
            return !existingEntry.getAvailabilityTrustFlag().equals(expectedTrustFlag);
        }

        @Override
        public void applyUpdate(ResourceAvailabilityLedgerDaily existingEntry, String eventId) {
            LedgerUpdateCondition.super.applyUpdate(existingEntry, eventId);
            existingEntry.setAvailabilityTrustFlag(expectedTrustFlag);
        }
    }

    public static class VersionCondition implements LedgerUpdateCondition {
        private final long minVersion;

        public VersionCondition(long minVersion) {
            this.minVersion = minVersion;
        }

        @Override
        public boolean shouldUpdate(ResourceAvailabilityLedgerDaily existingEntry) {
            return existingEntry.getCalculationVersion() < minVersion;
        }

        @Override
        public void applyUpdate(ResourceAvailabilityLedgerDaily existingEntry, String eventId) {
            LedgerUpdateCondition.super.applyUpdate(existingEntry, eventId);
            existingEntry.setCalculationVersion(minVersion);
        }
    }
}
