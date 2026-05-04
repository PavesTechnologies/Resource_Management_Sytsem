package com.service_imple.ledger_service_impl;

import com.entity.ledger_entities.ResourceAvailabilityLedgerDaily;
import com.entity.resource_entities.ResourceAvailabilityLedger;
import com.repo.availability_repo.ResourceAvailabilityLedgerRepository;
import com.repo.ledger_repo.ResourceAvailabilityLedgerDailyRepository;
import com.service_interface.ledger_service_interface.AllocationService;
import com.service_interface.ledger_service_interface.HolidayService;
import com.service_interface.ledger_service_interface.LeaveService;
import com.service_interface.ledger_service_interface.LedgerAvailabilityCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service("ledgerAvailabilityCalculationService")
@RequiredArgsConstructor
@Slf4j
public class LedgerAvailabilityCalculationServiceImpl implements LedgerAvailabilityCalculationService {

    private final ResourceAvailabilityLedgerRepository ledgerRepository;
    private final HolidayService holidayService;
    private final LeaveService leaveService;
    
    @Qualifier("ledgerAllocationService")
    private final AllocationService allocationService;
    
    private final ResourceAvailabilityLedgerDailyRepository dailyLedgerRepository;

    private static final int HOURS_PER_WORKING_DAY = 8;
    private static final Set<DayOfWeek> WEEKEND_DAYS = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    @Override
    @Transactional
    public void recalculateForDateRange(Long resourceId, LocalDate startDate, LocalDate endDate) {
        try {
            YearMonth startMonth = YearMonth.from(startDate);
            YearMonth endMonth = YearMonth.from(endDate);
            
            YearMonth currentMonth = startMonth;
            while (!currentMonth.isAfter(endMonth)) {
                calculateMonthlyAvailability(resourceId, currentMonth);
                currentMonth = currentMonth.plusMonths(1);
            }
        } catch (Exception e) {
            log.error("Failed to recalculate availability for resource {}: {}", resourceId, e.getMessage(), e);
            throw new RuntimeException("Failed to recalculate availability", e);
        }
    }

    @Transactional
    public ResourceAvailabilityLedger calculateMonthlyAvailability(Long resourceId, YearMonth yearMonth) {
        try {
            LocalDate monthStart = yearMonth.atDay(1);
            LocalDate monthEnd = yearMonth.atEndOfMonth();
            
            Set<LocalDate> workingDays = getWorkingDaysInRange(monthStart, monthEnd);
            Set<LocalDate> holidays = getHolidaysForMonth(yearMonth.getYear());
            Set<LocalDate> leaves = getLeavesForResourceForMonth(resourceId, yearMonth.getYear());
            
            int standardHours = workingDays.size() * HOURS_PER_WORKING_DAY;
            int holidayHours = (int) holidays.stream()
                .filter(workingDays::contains)
                .count() * HOURS_PER_WORKING_DAY;
            int leaveHours = (int) leaves.stream()
                .filter(date -> !holidays.contains(date))
                .filter(workingDays::contains)
                .count() * HOURS_PER_WORKING_DAY;
            
            AllocationService.AllocationData allocationData = allocationService.getAllocationDataForResourceForMonth(resourceId, yearMonth);
            int confirmedAllocHours = (allocationData.getConfirmedAllocationPercentage() * standardHours) / 100;
            int draftAllocHours = (allocationData.getDraftAllocationPercentage() * standardHours) / 100;
            
            int firmAvailableHours = standardHours - holidayHours - leaveHours - confirmedAllocHours;
            int projectedAvailableHours = firmAvailableHours - draftAllocHours;
            int totalAllocation = allocationData.getConfirmedAllocationPercentage() + allocationData.getDraftAllocationPercentage();
            int availablePercentage = standardHours > 0 ? Math.max(0, (firmAvailableHours * 100) / standardHours) : 0;
            
            boolean availabilityTrustFlag = holidayService.isApiHealthy() && leaveService.isApiHealthy();
            
            Optional<ResourceAvailabilityLedger> existing = ledgerRepository.findByResourceIdAndPeriodStart(resourceId, monthStart);
            ResourceAvailabilityLedger ledger = existing.orElseGet(() -> ResourceAvailabilityLedger.builder()
                .resource(null) // Will be set by repository
                .periodStart(monthStart)
                .periodEnd(monthEnd)
                .build());
            
            ledger.setStandardHours(standardHours);
            ledger.setHolidayHours(holidayHours);
            ledger.setLeaveHours(leaveHours);
            ledger.setConfirmedAllocHours(confirmedAllocHours);
            ledger.setDraftAllocHours(draftAllocHours);
            ledger.setTotalAllocation(totalAllocation);
            ledger.setAvailablePercentage(availablePercentage);
            ledger.setFirmAvailableHours(firmAvailableHours);
            ledger.setProjectedAvailableHours(projectedAvailableHours);
            ledger.setAvailabilityTrustFlag(availabilityTrustFlag);
            ledger.setLastCalculatedAt(LocalDateTime.now());
            
            return ledgerRepository.save(ledger);
            
        } catch (Exception e) {
            log.error("Monthly calculation error for resource {} for month {}: {}", resourceId, yearMonth, e.getMessage());
            throw new RuntimeException("Failed to calculate monthly availability", e);
        }
    }

    @Override
    @Transactional
    public void recalculateDailyWithIdempotency(Long resourceId, LocalDate date, String eventId) {
        YearMonth month = YearMonth.from(date);
        calculateMonthlyAvailability(resourceId, month);
        deriveDailyFromMonthly(resourceId, month);
    }

    @Transactional
    public void deriveDailyFromMonthly(Long resourceId, YearMonth month) {
        try {
            Optional<ResourceAvailabilityLedger> monthlyLedger = ledgerRepository.findByResourceIdAndPeriodStart(resourceId, month.atDay(1));
            if (monthlyLedger.isEmpty()) {
                return;
            }
            
            ResourceAvailabilityLedger monthly = monthlyLedger.get();
            LocalDate monthStart = month.atDay(1);
            LocalDate monthEnd = month.atEndOfMonth();
            
            List<ResourceAvailabilityLedgerDaily> dailyEntries = new ArrayList<>();
            LocalDate currentDate = monthStart;
            
            while (!currentDate.isAfter(monthEnd)) {
                if (isWorkingDay(currentDate)) {
                    ResourceAvailabilityLedgerDaily daily = createDailyFromMonthly(resourceId, currentDate, monthly);
                    dailyEntries.add(daily);
                }
                currentDate = currentDate.plusDays(1);
            }
            
            if (!dailyEntries.isEmpty()) {
                dailyLedgerRepository.saveAll(dailyEntries);
            }
        } catch (Exception e) {
            log.error("Failed to derive daily entries for resource {} month {}: {}", resourceId, month, e.getMessage());
        }
    }
    
    private ResourceAvailabilityLedgerDaily createDailyFromMonthly(Long resourceId, LocalDate date, ResourceAvailabilityLedger monthly) {
        int workingDaysInMonth = getWorkingDaysInRange(monthly.getPeriodStart(), monthly.getPeriodEnd()).size();
        
        int dailyStandardHours = HOURS_PER_WORKING_DAY;
        int dailyHolidayHours = isHoliday(date) ? HOURS_PER_WORKING_DAY : 0;
        int dailyLeaveHours = isLeave(date) && !isHoliday(date) ? HOURS_PER_WORKING_DAY : 0;
        
        int dailyConfirmedAllocHours = monthly.getConfirmedAllocHours() / workingDaysInMonth;
        int dailyDraftAllocHours = monthly.getDraftAllocHours() / workingDaysInMonth;
        
        int dailyFirmAvailable = dailyStandardHours - dailyHolidayHours - dailyLeaveHours - dailyConfirmedAllocHours;
        int dailyTotalAllocation = (dailyConfirmedAllocHours + dailyDraftAllocHours) * 100 / dailyStandardHours;
        int dailyAvailablePercentage = dailyStandardHours > 0 ? Math.max(0, (dailyFirmAvailable * 100) / dailyStandardHours) : 0;
        
        return ResourceAvailabilityLedgerDaily.builder()
                .resourceId(resourceId)
                .date(date)
                .standardHours(dailyStandardHours)
                .holidayHours(dailyHolidayHours)
                .leaveHours(dailyLeaveHours)
                .confirmedAllocHours(dailyConfirmedAllocHours)
                .draftAllocHours(dailyDraftAllocHours)
                .totalAllocationPercentage(dailyTotalAllocation)
                .availablePercentage(dailyAvailablePercentage)
                .isOverallocated(dailyTotalAllocation > 100)
                .overAllocationPercentage(Math.max(0, dailyTotalAllocation - 100))
                .availabilityTrustFlag(monthly.getAvailabilityTrustFlag())
                .calculationVersion(System.currentTimeMillis())
                .lastEventId(null)
                .build();
    }

    private Set<LocalDate> getWorkingDaysInRange(LocalDate startDate, LocalDate endDate) {
        Set<LocalDate> workingDays = new HashSet<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (isWorkingDay(current)) {
                workingDays.add(current);
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }
    
    private Set<LocalDate> getHolidaysForMonth(int year) {
        try {
            return holidayService.getHolidaysForYear(year);
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }
    
    private Set<LocalDate> getLeavesForResourceForMonth(Long resourceId, int year) {
        try {
            return leaveService.getApprovedLeaveForEmployee(resourceId, year);
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }
    
    private boolean isHoliday(LocalDate date) {
        try {
            return holidayService.getHolidaysForYear(date.getYear()).contains(date);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isLeave(LocalDate date) {
        try {
            return leaveService.getApprovedLeaveForEmployee(0L, date.getYear()).contains(date);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isWorkingDay(LocalDate date) {
        return !WEEKEND_DAYS.contains(date.getDayOfWeek());
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
            ledgerRepository.markDatesUntrustworthy(resourceId, startDate, endDate, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to mark entries as untrustworthy for resource {}: {}", resourceId, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceAvailabilityLedgerDaily> getAvailabilityForDateRange(Long resourceId, LocalDate startDate, LocalDate endDate) {
        try {
            return dailyLedgerRepository.findByResourceIdAndDateBetween(resourceId, startDate, endDate);
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
            return dailyLedgerRepository.findByResourceIdAndDate(resourceId, date);
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
            dailyLedgerRepository.deleteEntriesOlderThan(cutoffDate);
        } catch (Exception e) {
            log.error("Failed to cleanup old ledger entries: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAvailabilitySummary(Long resourceId, LocalDate startDate, LocalDate endDate) {
        try {
            List<ResourceAvailabilityLedgerDaily> entries = dailyLedgerRepository.findByResourceIdAndDateBetween(resourceId, startDate, endDate);
            
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
