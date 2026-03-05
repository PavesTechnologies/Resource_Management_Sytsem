package com.repo.allocation_repo;

import com.entity.allocation_entities.ResourceAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AllocationRepository extends JpaRepository<ResourceAllocation, UUID> {

    List<ResourceAllocation> findByResource_ResourceId(Long resourceId);
    
    List<ResourceAllocation> findByDemand_DemandId(UUID demandId);
    
    List<ResourceAllocation> findByProject_PmsProjectId(Long projectId);
    
    @Query("SELECT ra FROM ResourceAllocation ra WHERE ra.resource.resourceId = :resourceId " +
           "AND ra.allocationStatus IN ('PLANNED', 'ACTIVE') " +
           "AND ra.allocationStartDate <= :endDate " +
           "AND ra.allocationEndDate >= :startDate")
    List<ResourceAllocation> findConflictingAllocations(
            @Param("resourceId") Long resourceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT ra FROM ResourceAllocation ra WHERE ra.resource.resourceId IN :resourceIds " +
           "AND ra.allocationStatus IN ('PLANNED', 'ACTIVE') " +
           "AND ra.allocationStartDate <= :endDate " +
           "AND ra.allocationEndDate >= :startDate")
    List<ResourceAllocation> findConflictingAllocationsForResources(
            @Param("resourceIds") List<Long> resourceIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    @Query("SELECT ra FROM ResourceAllocation ra WHERE ra.resource.resourceId = :resourceId " +
           "AND ra.allocationStatus IN ('PLANNED', 'ACTIVE') " +
           "AND ra.allocationStartDate <= :date " +
           "AND ra.allocationEndDate >= :date")
    List<ResourceAllocation> findActiveAllocationsForResourceOnDate(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date);
}
