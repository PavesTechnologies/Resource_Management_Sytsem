package com.service_imple.ledger_service_impl;

import com.entity.ledger_entities.ResourceAvailabilityLedgerDaily;
import com.repo.ledger_repo.ResourceAvailabilityLedgerDailyRepository;
import com.service_interface.ledger_service_interface.AllocationService;
import com.service_interface.ledger_service_interface.HolidayService;
import com.service_interface.ledger_service_interface.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service("ledgerAvailabilityCalculationService")
@RequiredArgsConstructor
@Slf4j
public class AvailabilityCalculationService implements com.service_interface.ledger_service_interface.AvailabilityCalculationService {

    private final ResourceAvailabilityLedgerDailyRepository ledgerRepository;
    private final HolidayService holidayService;
    private final LeaveService leaveService;
    
    @Qualifier("ledgerAllocationService") // Correctly qualify the service
    private final AllocationService allocationService;

    private static final int HOURS_PER_WORKING_DAY = 8;
    private static final Set<DayOfWeek> WEEKEND_DAYS = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    @Override
    @Transactional
    public void recalculateForDateRange(Long resourceId, LocalDate startDate, LocalDate endDate) {
        try {
            List<ResourceAvailabilityLedgerDaily> ledgerEntries = new ArrayList<>();
            AtomicLong calculationVersion = new AtomicLong(System.currentTimeMillis());

            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                try {
                    ResourceAvailabilityLedgerDaily entry = calculateDailyAvailability(resourceId, currentDate, calculationVersion.get());
                    if (entry != null) {
                        ledgerEntries.add(entry);
                    }
                } catch (Exception e) {
                    log.error("Failed to calculate availability for resource {} on date {}: {}", 
                            resourceId, currentDate, e.getMessage());
                }
                currentDate = currentDate.plusDays(1);
            }

            if (!ledgerEntries.isEmpty()) {
                ledgerRepository.saveAll(ledgerEntries);
            }
        } catch (Exception e) {
            log.error("Failed to recalculate availability for resource {}: {}", resourceId, e.getMessage(), e);
            throw new RuntimeException("Failed to recalculate availability", e);
        }
    }

    @Override
    @Transactional
    public ResourceAvailabilityLedgerDaily calculateDailyAvailability(Long resourceId, LocalDate date, Long calculationVersion) {
        try {
            boolean isWorkingDay = isWorkingDay(date);
            if (!isWorkingDay) {
                return createNonWorkingDayEntry(resourceId, date, calculationVersion);
            }

            Set<LocalDate> holidays = getHolidaysForDate(date);
            Set<LocalDate> leaves = getLeavesForResourceAndDate(resourceId, date);

            boolean isHoliday = holidays.contains(date);
            boolean isLeave = leaves.contains(date);

            int standardHours = isWorkingDay ? HOURS_PER_WORKING_DAY : 0;
            int holidayHours = (isHoliday && isWorkingDay) ? HOURS_PER_WORKING_DAY : 0;
            int leaveHours = (isLeave && isWorkingDay && !isHoliday) ? HOURS_PER_WORKING_DAY : 0;

            com.service_interface.ledger_service_interface.AllocationService.AllocationData allocationData = 
                    allocationService.getAllocationDataForResourceAndDate(resourceId, date);
            
            int confirmedAllocHours = (allocationData.getConfirmedAllocationPercentage() * HOURS_PER_WORKING_DAY) / 100;
            int draftAllocHours = (allocationData.getDraftAllocationPercentage() * HOURS_PER_WORKING_DAY) / 100;

            int firmAvailableHours = standardHours - holidayHours - leaveHours - confirmedAllocHours;
            
            int totalAllocationPercentage = allocationData.getConfirmedAllocationPercentage() + allocationData.getDraftAllocationPercentage();
            int availablePercentage = standardHours > 0 ? Math.max(0, (firmAvailableHours * 100) / standardHours) : 0;

            boolean isOverallocated = totalAllocationPercentage > 100;
            int overAllocationPercentage = Math.max(0, totalAllocationPercentage - 100);

            boolean availabilityTrustFlag = holidayService.isApiHealthy() && leaveService.isApiHealthy();

            return ResourceAvailabilityLedgerDaily.builder()
                    .resourceId(resourceId)
                    .date(date)
                    .standardHours(standardHours)
                    .holidayHours(holidayHours)
                    .leaveHours(leaveHours)
                    .confirmedAllocHours(confirmedAllocHours)
                    .draftAllocHours(draftAllocHours)
                    .totalAllocationPercentage(totalAllocationPercentage)
                    .availablePercentage(availablePercentage)
                    .isOverallocated(isOverallocated)
                    .overAllocationPercentage(overAllocationPercentage)
                    .availabilityTrustFlag(availabilityTrustFlag)
                    .calculationVersion(calculationVersion)
                    .lastEventId(null)
                    .build();

        } catch (Exception e) {
            log.error("Daily calculation error for resource {} on date {}: {}", resourceId, date, e.getMessage());
            return createErrorEntry(resourceId, date, calculationVersion, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void recalculateDailyWithIdempotency(Long resourceId, LocalDate date, String eventId) {
        try {
            Optional<ResourceAvailabilityLedgerDaily> existing = ledgerRepository.findByResourceIdAndDate(resourceId, date);
            if (existing.isPresent() && eventId != null && eventId.equals(existing.get().getLastEventId())) {
                return;
            }

            long calculationVersion = System.currentTimeMillis();
            ResourceAvailabilityLedgerDaily entry = calculateDailyAvailability(resourceId, date, calculationVersion);
            
            if (entry != null) {
                entry.setLastEventId(eventId);
                ledgerRepository.save(entry);
            }
        } catch (Exception e) {
            log.error("Idempotent recalculation failed for resource {} on date {} with event {}: {}", 
                    resourceId, date, eventId, e.getMessage());
            throw new RuntimeException("Failed to recalculate availability with idempotency", e);
        }
    }

    private ResourceAvailabilityLedgerDaily createNonWorkingDayEntry(Long resourceId, LocalDate date, Long calculationVersion) {
        return ResourceAvailabilityLedgerDaily.builder()
                .resourceId(resourceId)
                .date(date)
                .standardHours(0)
                .holidayHours(0)
                .leaveHours(0)
                .confirmedAllocHours(0)
                .draftAllocHours(0)
                .totalAllocationPercentage(0)
                .availablePercentage(0)
                .isOverallocated(false)
                .overAllocationPercentage(0)
                .availabilityTrustFlag(true)
                .calculationVersion(calculationVersion)
                .lastEventId(null)
                .build();
    }

    private ResourceAvailabilityLedgerDaily createErrorEntry(Long resourceId, LocalDate date, Long calculationVersion, String errorMessage) {
        return ResourceAvailabilityLedgerDaily.builder()
                .resourceId(resourceId)
                .date(date)
                .standardHours(0)
                .holidayHours(0)
                .leaveHours(0)
                .confirmedAllocHours(0)
                .draftAllocHours(0)
                .totalAllocationPercentage(0)
                .availablePercentage(0)
                .isOverallocated(false)
                .overAllocationPercentage(0)
                .availabilityTrustFlag(false)
                .calculationVersion(calculationVersion)
                .lastEventId(null)
                .build();
    }

    private boolean isWorkingDay(LocalDate date) {
        return !WEEKEND_DAYS.contains(date.getDayOfWeek());
    }

    private Set<LocalDate> getHolidaysForDate(LocalDate date) {
        try {
            return holidayService.getHolidaysForYear(date.getYear());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    private Set<LocalDate> getLeavesForResourceAndDate(Long resourceId, LocalDate date) {
        try {
            return leaveService.getApprovedLeaveForEmployee(resourceId, date.getYear());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    @Override
    @Transactional
    public void recalculateForSingleDate(Long resourceId, LocalDate date) {
        recalculateDailyWithIdempotency(resourceId, date, null);
    }

    @Override
    @Transactional
    public void markAsUntrustworthy(Long resourceId, LocalDate startDate, LocalDate endDate) {
        try {
            ledgerRepository.markAsUntrustworthy(resourceId, startDate, endDate);
        } catch (Exception e) {
            log.error("Failed to mark entries as untrustworthy for resource {}: {}", resourceId, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceAvailabilityLedgerDaily> getAvailabilityForDateRange(Long resourceId, LocalDate startDate, LocalDate endDate) {
        try {
            return ledgerRepository.findByResourceIdAndDateBetween(resourceId, startDate, endDate);
        } catch (Exception e) {
            log.error("Failed to fetch availability for resource {} from {} to {}: {}", 
                    resourceId, startDate, endDate, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResourceAvailabilityLedgerDaily> getAvailabilityForDate(Long resourceId, LocalDate date) {
        try {
            return ledgerRepository.findByResourceIdAndDate(resourceId, date);
        } catch (Exception e) {
            log.error("Failed to fetch availability for resource {} on date {}: {}", 
                    resourceId, date, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void cleanupOldEntries(LocalDate cutoffDate) {
        try {
            ledgerRepository.deleteEntriesOlderThan(cutoffDate);
        } catch (Exception e) {
            log.error("Failed to cleanup old ledger entries: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAvailabilitySummary(Long resourceId, LocalDate startDate, LocalDate endDate) {
        try {
            List<ResourceAvailabilityLedgerDaily> entries = ledgerRepository.findByResourceIdAndDateBetween(resourceId, startDate, endDate);
            
            if (entries.isEmpty()) {
                return Map.of(
                        "totalDays", 0,
                        "workingDays", 0,
                        "availableDays", 0,
                        "overallocatedDays", 0,
                        "averageAvailability", 0
                );
            }

            long totalDays = entries.size();
            long workingDays = entries.stream().mapToLong(e -> e.getStandardHours() > 0 ? 1 : 0).sum();
            long availableDays = entries.stream().mapToLong(e -> e.getAvailablePercentage() > 0 ? 1 : 0).sum();
            long overallocatedDays = entries.stream().mapToLong(e -> e.getIsOverallocated() ? 1 : 0).sum();
            double averageAvailability = entries.stream()
                    .filter(e -> e.getStandardHours() > 0)
                    .mapToInt(e -> e.getAvailablePercentage())
                    .average()
                    .orElse(0.0);

            return Map.of(
                    "totalDays", totalDays,
                    "workingDays", workingDays,
                    "availableDays", availableDays,
                    "overallocatedDays", overallocatedDays,
                    "averageAvailability", Math.round(averageAvailability)
            );

        } catch (Exception e) {
            log.error("Failed to get availability summary for resource {}: {}", resourceId, e.getMessage());
            return Map.of(
                    "totalDays", 0,
                    "workingDays", 0,
                    "availableDays", 0,
                    "overallocatedDays", 0,
                    "averageAvailability", 0
            );
        }
    }
}
