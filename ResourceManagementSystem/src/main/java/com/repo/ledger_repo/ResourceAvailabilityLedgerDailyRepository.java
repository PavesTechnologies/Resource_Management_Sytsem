package com.repo.ledger_repo;

import com.entity.ledger_entities.ResourceAvailabilityLedgerDaily;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceAvailabilityLedgerDailyRepository extends JpaRepository<ResourceAvailabilityLedgerDaily, Long>, 
                                                                   JpaSpecificationExecutor<ResourceAvailabilityLedgerDaily> {

    Optional<ResourceAvailabilityLedgerDaily> findByResourceIdAndDate(Long resourceId, LocalDate date);

    List<ResourceAvailabilityLedgerDaily> findByResourceIdAndDateBetween(Long resourceId, LocalDate startDate, LocalDate endDate);

    List<ResourceAvailabilityLedgerDaily> findByResourceIdInAndDateBetween(List<Long> resourceIds, LocalDate startDate, LocalDate endDate);

    Page<ResourceAvailabilityLedgerDaily> findByResourceId(Long resourceId, Pageable pageable);

    @Query("SELECT rald FROM ResourceAvailabilityLedgerDaily rald WHERE rald.date = :date AND rald.resourceId = :resourceId")
    Optional<ResourceAvailabilityLedgerDaily> findByResourceAndDateOptimized(@Param("resourceId") Long resourceId, @Param("date") LocalDate date);

    @Query("SELECT rald FROM ResourceAvailabilityLedgerDaily rald WHERE rald.date BETWEEN :startDate AND :endDate AND rald.availabilityTrustFlag = false")
    List<ResourceAvailabilityLedgerDaily> findUntrustworthyEntriesInDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(rald) FROM ResourceAvailabilityLedgerDaily rald WHERE rald.resourceId = :resourceId AND rald.date BETWEEN :startDate AND :endDate AND rald.isOverallocated = true")
    Long countOverallocatedDaysForResource(@Param("resourceId") Long resourceId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT rald FROM ResourceAvailabilityLedgerDaily rald WHERE rald.date < :cutoffDate")
    List<ResourceAvailabilityLedgerDaily> findEntriesOlderThan(@Param("cutoffDate") LocalDate cutoffDate);

    @Modifying
    @Query("DELETE FROM ResourceAvailabilityLedgerDaily rald WHERE rald.date < :cutoffDate")
    int deleteEntriesOlderThan(@Param("cutoffDate") LocalDate cutoffDate);

    @Query("SELECT DISTINCT rald.resourceId FROM ResourceAvailabilityLedgerDaily rald WHERE rald.date BETWEEN :startDate AND :endDate")
    List<Long> findActiveResourceIdsInDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT rald FROM ResourceAvailabilityLedgerDaily rald WHERE rald.lastEventId = :eventId")
    List<ResourceAvailabilityLedgerDaily> findByLastEventId(@Param("eventId") String eventId);

    @Modifying
    @Query("UPDATE ResourceAvailabilityLedgerDaily rald SET rald.availabilityTrustFlag = false WHERE rald.resourceId = :resourceId AND rald.date BETWEEN :startDate AND :endDate")
    int markAsUntrustworthy(@Param("resourceId") Long resourceId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = "INSERT INTO resource_availability_ledger_daily (resource_id, date, standard_hours, holiday_hours, leave_hours, confirmed_alloc_hours, draft_alloc_hours, total_allocation_percentage, available_percentage, is_overallocated, over_allocation_percentage, availability_trust_flag, calculation_version, last_event_id, created_at, updated_at, version) " +
           "VALUES (:resourceId, :date, :standardHours, :holidayHours, :leaveHours, :confirmedAllocHours, :draftAllocHours, :totalAllocationPercentage, :availablePercentage, :isOverallocated, :overAllocationPercentage, :availabilityTrustFlag, :calculationVersion, :lastEventId, :createdAt, :updatedAt, :version) " +
           "ON DUPLICATE KEY UPDATE " +
           "standard_hours = VALUES(standard_hours), " +
           "holiday_hours = VALUES(holiday_hours), " +
           "leave_hours = VALUES(leave_hours), " +
           "confirmed_alloc_hours = VALUES(confirmed_alloc_hours), " +
           "draft_alloc_hours = VALUES(draft_alloc_hours), " +
           "total_allocation_percentage = VALUES(total_allocation_percentage), " +
           "available_percentage = VALUES(available_percentage), " +
           "is_overallocated = VALUES(is_overallocated), " +
           "over_allocation_percentage = VALUES(over_allocation_percentage), " +
           "availability_trust_flag = VALUES(availability_trust_flag), " +
           "calculation_version = VALUES(calculation_version), " +
           "last_event_id = VALUES(last_event_id), " +
           "updated_at = VALUES(updated_at), " +
           "version = version + 1", nativeQuery = true)
    void upsertLedgerEntry(
        @Param("resourceId") Long resourceId,
        @Param("date") LocalDate date,
        @Param("standardHours") Integer standardHours,
        @Param("holidayHours") Integer holidayHours,
        @Param("leaveHours") Integer leaveHours,
        @Param("confirmedAllocHours") Integer confirmedAllocHours,
        @Param("draftAllocHours") Integer draftAllocHours,
        @Param("totalAllocationPercentage") Integer totalAllocationPercentage,
        @Param("availablePercentage") Integer availablePercentage,
        @Param("isOverallocated") Boolean isOverallocated,
        @Param("overAllocationPercentage") Integer overAllocationPercentage,
        @Param("availabilityTrustFlag") Boolean availabilityTrustFlag,
        @Param("calculationVersion") Long calculationVersion,
        @Param("lastEventId") String lastEventId,
        @Param("createdAt") java.time.LocalDateTime createdAt,
        @Param("updatedAt") java.time.LocalDateTime updatedAt,
        @Param("version") Long version
    );
}
