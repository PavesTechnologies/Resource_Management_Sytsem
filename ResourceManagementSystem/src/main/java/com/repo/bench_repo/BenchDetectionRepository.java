package com.repo.bench_repo;

import com.entity.bench.ResourceState;
import com.entity.resource_entities.Resource;
import com.entity_enums.bench.StateType;
import com.entity_enums.bench.SubState;
import com.entity_enums.bench.BenchReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BenchDetectionRepository extends JpaRepository<ResourceState, Long> {

    /**
     * Find resources eligible for bench detection
     * Resources with NO active allocation and meeting eligibility criteria
     */
    @Query("""
        SELECT DISTINCT r.resourceId
        FROM Resource r
        LEFT JOIN ResourceAllocation a 
          ON a.resource = r
          AND a.allocationStatus = 'ACTIVE'
        WHERE a.id IS NULL
          AND r.activeFlag = true
          AND r.allocationAllowed = true
          AND (r.noticeStartDate IS NULL OR r.noticeStartDate > :currentDate)
        """)
    List<Long> findBenchEligibleResources(@Param("currentDate") LocalDate currentDate);

    /**
     * Find current active state for a resource by resourceId and currentFlag
     */
    Optional<ResourceState> findByResourceIdAndCurrentFlagTrue(Long resourceId);

    /**
     * Find current active state for a resource
     */
    @Query("""
        SELECT rs
        FROM ResourceState rs
        WHERE rs.resourceId = :resourceId
          AND rs.currentFlag = true
        """)
    Optional<ResourceState> findCurrentState(@Param("resourceId") Long resourceId);

    /**
     * Find current bench state for a resource
     */
    @Query("""
        SELECT rs
        FROM ResourceState rs
        WHERE rs.resourceId = :resourceId
          AND rs.currentFlag = true
          AND rs.stateType = 'BENCH'
        """)
    Optional<ResourceState> findCurrentBenchState(@Param("resourceId") Long resourceId);

    /**
     * Find all resources currently in bench state
     */
    @Query("""
        SELECT rs.resourceId
        FROM ResourceState rs
        WHERE rs.stateType = 'BENCH'
          AND rs.currentFlag = true
        """)
    List<Long> findResourcesInBench();

    /**
     * Check if resource has any active allocations
     */
    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
        FROM ResourceAllocation a
        WHERE a.resource.resourceId = :resourceId
          AND a.allocationStatus = 'ACTIVE'
        """)
    boolean hasActiveAllocations(@Param("resourceId") Long resourceId);

    /**
     * Get current project state for a resource
     */
    @Query("""
        SELECT rs
        FROM ResourceState rs
        WHERE rs.resourceId = :resourceId
          AND rs.currentFlag = true
          AND rs.stateType = 'PROJECT'
        """)
    Optional<ResourceState> findCurrentProjectState(@Param("resourceId") Long resourceId);

    /**
     * Get all bench resources with complete resource details for frontend
     */
    @Query("""
        SELECT r
        FROM ResourceState rs
        JOIN Resource r ON rs.resourceId = r.resourceId
        WHERE rs.stateType = 'BENCH'
          AND rs.currentFlag = true
          AND r.activeFlag = true
        ORDER BY rs.benchStartDate ASC
        """)
    List<Resource> findAllBenchResources();

    /**
     * Get bench resources by sub-state
     */
    @Query("""
        SELECT r
        FROM ResourceState rs
        JOIN Resource r ON rs.resourceId = r.resourceId
        WHERE rs.stateType = 'BENCH'
          AND rs.currentFlag = true
          AND r.activeFlag = true
          AND rs.subState = :subState
        ORDER BY rs.benchStartDate ASC
        """)
    List<Resource> findBenchResourcesBySubState(@Param("subState") SubState subState);

    /**
     * Get bench resources by bench reason
     */
    @Query("""
        SELECT r
        FROM ResourceState rs
        JOIN Resource r ON rs.resourceId = r.resourceId
        WHERE rs.stateType = 'BENCH'
          AND rs.currentFlag = true
          AND r.activeFlag = true
          AND rs.benchReason = :benchReason
        ORDER BY rs.benchStartDate ASC
        """)
    List<Resource> findBenchResourcesByReason(@Param("benchReason") BenchReason benchReason);

    /**
     * Get bench resources by skill group
     */
    @Query("""
        SELECT r
        FROM ResourceState rs
        JOIN Resource r ON rs.resourceId = r.resourceId
        WHERE rs.stateType = 'BENCH'
          AND rs.currentFlag = true
          AND r.activeFlag = true
          AND r.primarySkillGroup = :skillGroup
        ORDER BY rs.benchStartDate ASC
        """)
    List<Resource> findBenchResourcesBySkillGroup(@Param("skillGroup") String skillGroup);

    /**
     * Get bench statistics
     */
    @Query("""
        SELECT COUNT(rs)
        FROM ResourceState rs
        WHERE rs.stateType = 'BENCH'
          AND rs.currentFlag = true
        """)
    long getTotalBenchCount();

    /**
     * Get bench count by sub-state
     */
    @Query("""
        SELECT rs.subState, COUNT(rs)
        FROM ResourceState rs
        WHERE rs.stateType = 'BENCH'
          AND rs.currentFlag = true
        GROUP BY rs.subState
        """)
    List<Object[]> getBenchCountBySubState();

    /**
     * Get bench count by reason
     */
    @Query("""
        SELECT rs.benchReason, COUNT(rs)
        FROM ResourceState rs
        WHERE rs.stateType = 'BENCH'
          AND rs.currentFlag = true
        GROUP BY rs.benchReason
        """)
    List<Object[]> getBenchCountByReason();

    /**
     * Get bench resources with skill details for bench endpoint
     * Bench resources are those with sub-states: READY, NOT_AVAILABLE, LOW_UTILIZATION
     */
    @Query("""
        SELECT r, rs.benchStartDate, rs.subState
        FROM ResourceState rs
        JOIN Resource r ON rs.resourceId = r.resourceId
        WHERE rs.stateType = 'BENCH'
          AND rs.currentFlag = true
          AND r.activeFlag = true
          AND rs.subState IN ('READY', 'NOT_AVAILABLE', 'LOW_UTILIZATION')
        ORDER BY rs.benchStartDate ASC
        """)
    List<Object[]> findBenchResourcesWithDetails();

    /**
     * Get pool resources with skill details for pool endpoint
     * Pool resources are those with sub-states: TRAINING_POOL, SHADOW, COE, RND, TRAINING
     */
    @Query("""
        SELECT r, rs.benchStartDate, rs.subState
        FROM ResourceState rs
        JOIN Resource r ON rs.resourceId = r.resourceId
        WHERE rs.stateType = 'BENCH'
          AND rs.currentFlag = true
          AND r.activeFlag = true
          AND rs.subState IN ('TRAINING_POOL', 'SHADOW', 'COE', 'RND', 'TRAINING')
        ORDER BY rs.benchStartDate ASC
        """)
    List<Object[]> findPoolResourcesWithDetails();

    /**
     * Count resources by state type
     */
    @Query("""
        SELECT COUNT(rs)
        FROM ResourceState rs
        WHERE rs.stateType = :stateType
          AND rs.currentFlag = true
        """)
    long countByStateType(@Param("stateType") StateType stateType);

    /**
     * Count resources by state type and sub state
     */
    @Query("""
        SELECT COUNT(rs)
        FROM ResourceState rs
        WHERE rs.stateType = :stateType
          AND rs.subState = :subState
          AND rs.currentFlag = true
        """)
    long countByStateTypeAndSubState(@Param("stateType") StateType stateType, @Param("subState") SubState subState);

    /**
     * Count bench resources older than specified days
     */
    @Query("""
        SELECT COUNT(rs)
        FROM ResourceState rs
        WHERE rs.stateType = 'BENCH'
          AND rs.currentFlag = true
          AND rs.subState IN ('READY', 'NOT_AVAILABLE', 'LOW_UTILIZATION')
          AND rs.benchStartDate <= :cutoffDate
        """)
    long countBenchResourcesOlderThanDays(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Count pool resources older than specified days
     */
    @Query("""
        SELECT COUNT(rs)
        FROM ResourceState rs
        WHERE rs.stateType = 'BENCH'
          AND rs.currentFlag = true
          AND rs.subState IN ('TRAINING_POOL', 'SHADOW', 'COE', 'RND', 'TRAINING')
          AND rs.benchStartDate <= :cutoffDate
        """)
    long countPoolResourcesOlderThanDays(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Count bench resources by sub-state (bench sub-states: READY, NOT_AVAILABLE, LOW_UTILIZATION)
     */
    @Query("""
        SELECT COUNT(rs)
        FROM ResourceState rs
        WHERE rs.stateType = 'BENCH'
          AND rs.currentFlag = true
          AND rs.subState IN ('READY', 'NOT_AVAILABLE', 'LOW_UTILIZATION')
        """)
    long countBenchResources();

    /**
     * Count pool resources by sub-state (pool sub-states: TRAINING_POOL, SHADOW, COE, RND, TRAINING)
     */
    @Query("""
        SELECT COUNT(rs)
        FROM ResourceState rs
        WHERE rs.stateType = 'BENCH'
          AND rs.currentFlag = true
          AND rs.subState IN ('TRAINING_POOL', 'SHADOW', 'COE', 'RND', 'TRAINING')
        """)
    long countPoolResources();
}
