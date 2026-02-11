package com.repo.availability_repo;

import com.entity.availability_entities.ResourceAvailabilityLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceAvailabilityLedgerRepository extends JpaRepository<ResourceAvailabilityLedger, Long> {

    @Query("SELECT ral FROM ResourceAvailabilityLedger ral WHERE ral.resource.resourceId = :resourceId " +
           "AND ral.periodStart = :periodStart")
    Optional<ResourceAvailabilityLedger> findByResourceIdAndPeriodStart(@Param("resourceId") Long resourceId, 
                                                                      @Param("periodStart") LocalDate periodStart);

    @Query("SELECT ral FROM ResourceAvailabilityLedger ral WHERE ral.resource.resourceId = :resourceId " +
           "AND ral.periodStart BETWEEN :startDate AND :endDate ORDER BY ral.periodStart")
    List<ResourceAvailabilityLedger> findByResourceIdAndPeriodStartBetweenOrderByPeriodStart(
            @Param("resourceId") Long resourceId, 
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate);

    List<ResourceAvailabilityLedger> findByPeriodStartBetweenOrderByPeriodStart(LocalDate startDate, LocalDate endDate);

    @Query("SELECT ral FROM ResourceAvailabilityLedger ral WHERE ral.resource.resourceId = :resourceId " +
           "AND ral.periodStart <= :date AND ral.periodEnd >= :date")
    Optional<ResourceAvailabilityLedger> findByResourceIdAndDate(@Param("resourceId") Long resourceId, @Param("date") LocalDate date);

    @Query("SELECT ral FROM ResourceAvailabilityLedger ral WHERE ral.availabilityTrustFlag = false " +
           "AND ral.lastCalculatedAt < :cutoffTime")
    List<ResourceAvailabilityLedger> findUntrustworthyCalculationsBefore(@Param("cutoffTime") java.time.LocalDateTime cutoffTime);

    @Query("SELECT COUNT(ral) FROM ResourceAvailabilityLedger ral WHERE ral.availabilityTrustFlag = true " +
           "AND ral.periodStart = :periodStart")
    long countTrustworthyCalculationsForPeriod(@Param("periodStart") LocalDate periodStart);

    @Modifying
    @Query("DELETE FROM ResourceAvailabilityLedger ral WHERE ral.resource.resourceId = :resourceId " +
           "AND ral.periodStart = :periodStart")
    void deleteByResourceIdAndPeriodStart(@Param("resourceId") Long resourceId, 
                                          @Param("periodStart") LocalDate periodStart);

    @Modifying
    @Query("DELETE FROM ResourceAvailabilityLedger ral WHERE ral.resource.resourceId = :resourceId " +
           "AND ral.periodStart >= :startDate")
    void deleteByResourceIdAndPeriodStartGreaterThanOrEqual(@Param("resourceId") Long resourceId, 
                                                          @Param("startDate") LocalDate startDate);

    @Modifying
    @Query("DELETE FROM ResourceAvailabilityLedger ral WHERE ral.resource.resourceId = :resourceId")
    void deleteByResourceId(@Param("resourceId") Long resourceId);
}
