package com.repo.timeline_repo;

import com.service_imple.availability_service_impl.projection.ResourceTimelineProjection;
import com.service_imple.availability_service_impl.projection.TimelineKpiProjection;
import com.service_imple.availability_service_impl.projection.AllocationTimelineProjection;
import com.service_imple.availability_service_impl.projection.CurrentProjectProjection;
import com.service_imple.availability_service_impl.projection.CurrentAllocationProjection;
import com.entity.resource_entities.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ResourceTimelineRepository extends JpaRepository<Resource, Long> {

    // Window Mode Query (when dates are provided)
    @Query(value = """
        SELECT
            r.resource_id as id,
            r.full_name as fullName,
            r.designation as designation,
            r.working_location as workingLocation,
            r.experiance as experiance,
            r.employment_type as employmentType,
            COALESCE(
                SUM(
                    (
                        DATEDIFF(
                            LEAST(ra.allocation_end_date, :endDate),
                            GREATEST(ra.allocation_start_date, :startDate)
                        ) + 1
                    ) * ra.allocation_percentage
                ) / (DATEDIFF(:endDate, :startDate) + 1),
                0
            ) AS avgAllocation,
            r.notice_start_date as noticeStartDate,
            r.notice_end_date as noticeEndDate,
            r.allocation_allowed as allocationAllowed
        FROM resource r
        LEFT JOIN resource_allocation ra
            ON ra.resource_id = r.resource_id
            AND ra.allocation_status IN ('ACTIVE', 'PLANNED')
            AND ra.allocation_start_date <= :endDate
            AND ra.allocation_end_date >= :startDate
        WHERE r.active_flag = 1
        AND (:designation IS NULL OR r.designation = :designation)
        AND (:location IS NULL OR r.working_location = :location)
        AND (:employmentType IS NULL OR r.employment_type = :employmentType)
        AND (:minExp IS NULL OR r.experiance >= :minExp)
        AND (:maxExp IS NULL OR r.experiance <= :maxExp)
        AND (:search IS NULL OR LOWER(r.full_name) LIKE LOWER(CONCAT('%', :search, '%')))
        GROUP BY r.resource_id
        ORDER BY r.full_name
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<ResourceTimelineProjection> getResourceTimelineWindow(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("designation") String designation,
        @Param("location") String location,
        @Param("employmentType") String employmentType,
        @Param("minExp") Integer minExp,
        @Param("maxExp") Integer maxExp,
        @Param("search") String search,
        @Param("size") Integer size,
        @Param("offset") Integer offset
    );

    // Full History Mode Query (when dates are null)
    @Query(value = """
        SELECT
            r.resource_id as id,
            r.full_name as fullName,
            r.designation as designation,
            r.working_location as workingLocation,
            r.experiance as experiance,
            r.employment_type as employmentType,
            COALESCE(
                CASE 
                    WHEN SUM(DATEDIFF(ra.allocation_end_date, ra.allocation_start_date) + 1) = 0 THEN 0
                    ELSE SUM(
                        (DATEDIFF(ra.allocation_end_date, ra.allocation_start_date) + 1)
                        * ra.allocation_percentage
                    ) / SUM(DATEDIFF(ra.allocation_end_date, ra.allocation_start_date) + 1)
                END,
                0
            ) AS avgAllocation,
            r.notice_start_date as noticeStartDate,
            r.notice_end_date as noticeEndDate,
            r.allocation_allowed as allocationAllowed
        FROM resource r
        LEFT JOIN resource_allocation ra
            ON ra.resource_id = r.resource_id
            AND ra.allocation_status IN ('ACTIVE', 'PLANNED')
        WHERE r.active_flag = 1
        AND (:designation IS NULL OR r.designation = :designation)
        AND (:location IS NULL OR r.working_location = :location)
        AND (:employmentType IS NULL OR r.employment_type = :employmentType)
        AND (:minExp IS NULL OR r.experiance >= :minExp)
        AND (:maxExp IS NULL OR r.experiance <= :maxExp)
        AND (
                    :search IS NULL OR
                    LOWER(r.full_name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                    LOWER(r.designation) LIKE LOWER(CONCAT('%', :search, '%'))
                )
        GROUP BY r.resource_id
        ORDER BY r.full_name
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<ResourceTimelineProjection> getResourceTimelineFullHistory(
        @Param("designation") String designation,
        @Param("location") String location,
        @Param("employmentType") String employmentType,
        @Param("minExp") Integer minExp,
        @Param("maxExp") Integer maxExp,
        @Param("search") String search,
        @Param("size") Integer size,
        @Param("offset") Integer offset
    );

    // Window Mode Count Query
    @Query(value = """
        SELECT COUNT(DISTINCT r.resource_id) as totalCount
        FROM resource r
        LEFT JOIN resource_allocation ra
            ON ra.resource_id = r.resource_id
            AND ra.allocation_status IN ('ACTIVE', 'PLANNED')
            AND ra.allocation_start_date <= :endDate
            AND ra.allocation_end_date >= :startDate
        WHERE r.active_flag = 1
        AND (:designation IS NULL OR r.designation = :designation)
        AND (:location IS NULL OR r.working_location = :location)
        AND (:employmentType IS NULL OR r.employment_type = :employmentType)
        AND (:minExp IS NULL OR r.experiance >= :minExp)
        AND (:maxExp IS NULL OR r.experiance <= :maxExp)
        """, nativeQuery = true)
    Long getResourceTimelineWindowCount(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("designation") String designation,
        @Param("location") String location,
        @Param("employmentType") String employmentType,
        @Param("minExp") Integer minExp,
        @Param("maxExp") Integer maxExp
    );

    // Full History Mode Count Query
    @Query(value = """
        SELECT COUNT(DISTINCT r.resource_id) as totalCount
        FROM resource r
        LEFT JOIN resource_allocation ra
            ON ra.resource_id = r.resource_id
            AND ra.allocation_status IN ('ACTIVE', 'PLANNED')
        WHERE r.active_flag = 1
        AND (:designation IS NULL OR r.designation = :designation)
        AND (:location IS NULL OR r.working_location = :location)
        AND (:employmentType IS NULL OR r.employment_type = :employmentType)
        AND (:minExp IS NULL OR r.experiance >= :minExp)
        AND (:maxExp IS NULL OR r.experiance <= :maxExp)
        """, nativeQuery = true)
    Long getResourceTimelineFullHistoryCount(
        @Param("designation") String designation,
        @Param("location") String location,
        @Param("employmentType") String employmentType,
        @Param("minExp") Integer minExp,
        @Param("maxExp") Integer maxExp
    );

    // Get current projects for resources (active allocations as of TODAY)
    @Query(value = """
        SELECT
            ra.resource_id as resourceId,
            p.name as projectName
        FROM resource_allocation ra
        JOIN project p ON p.pms_project_id = ra.project_id
        WHERE ra.resource_id IN :resourceIds
        AND ra.allocation_status IN ('ACTIVE', 'PLANNED')
        AND CURRENT_DATE BETWEEN ra.allocation_start_date AND ra.allocation_end_date
        ORDER BY ra.resource_id, p.name
        """, nativeQuery = true)
    List<CurrentProjectProjection> getCurrentProjects(@Param("resourceIds") List<Long> resourceIds);

    // Get current allocation for resources (sum of allocations active TODAY)
    @Query(value = """
        SELECT
            ra.resource_id as resourceId,
            SUM(CASE WHEN CURRENT_DATE BETWEEN ra.allocation_start_date AND ra.allocation_end_date
                THEN ra.allocation_percentage ELSE 0 END) as currentAllocation
        FROM resource_allocation ra
        WHERE ra.resource_id IN :resourceIds
        AND ra.allocation_status IN ('ACTIVE', 'PLANNED')
        GROUP BY ra.resource_id
        """, nativeQuery = true)
    List<CurrentAllocationProjection> getCurrentAllocations(@Param("resourceIds") List<Long> resourceIds);

    // Allocation Timeline Query
    @Query(value = """
        SELECT
            ra.resource_id as resourceId,
            p.name as project,
            ra.allocation_start_date as startDate,
            ra.allocation_end_date as endDate,
            ra.allocation_percentage as allocation,
            ra.allocation_status as allocationStatus
        FROM resource_allocation ra
        JOIN project p ON p.pms_project_id = ra.project_id
        WHERE ra.resource_id IN :resourceIds
        AND ra.allocation_status IN ('ACTIVE', 'PLANNED')
        ORDER BY ra.resource_id, ra.allocation_start_date
        """, nativeQuery = true)
    List<AllocationTimelineProjection> getAllocationTimeline(@Param("resourceIds") List<Long> resourceIds);

    // Window Mode KPI Query
    @Query(value = """
        SELECT
            COUNT(DISTINCT r.resource_id) as totalResources,
            SUM(CASE 
                WHEN resource_avg_allocation <= 20 THEN 1 ELSE 0 END) as fullyAvailable,
            SUM(CASE 
                WHEN resource_avg_allocation > 20 AND resource_avg_allocation <= 70 THEN 1 ELSE 0 END) as partiallyAvailable,
            SUM(CASE 
                WHEN resource_avg_allocation > 70 AND resource_avg_allocation <= 100 THEN 1 ELSE 0 END) as fullyAllocated,
            SUM(CASE 
                WHEN resource_avg_allocation > 100 THEN 1 ELSE 0 END) as overAllocated,
            COUNT(CASE 
                WHEN r.notice_start_date IS NOT NULL AND r.notice_end_date IS NOT NULL
                AND CURRENT_DATE BETWEEN r.notice_start_date AND r.notice_end_date 
                THEN 1 ELSE NULL END) as noticePeriodResources,
            COUNT(CASE 
                WHEN r.notice_start_date IS NOT NULL AND r.notice_end_date IS NOT NULL
                AND CURRENT_DATE BETWEEN r.notice_start_date AND r.notice_end_date
                AND resource_avg_allocation <= 50 
                THEN 1 ELSE NULL END) as availableNoticePeriodResources
        FROM (
            SELECT
                r.resource_id,
                COALESCE(
                    SUM(
                        (
                            DATEDIFF(
                                LEAST(ra.allocation_end_date, :endDate),
                                GREATEST(ra.allocation_start_date, :startDate)
                            ) + 1
                        ) * ra.allocation_percentage
                    ) / (DATEDIFF(:endDate, :startDate) + 1),
                    0
                ) AS resource_avg_allocation
            FROM resource r
            LEFT JOIN resource_allocation ra
                ON ra.resource_id = r.resource_id
                AND ra.allocation_status IN ('ACTIVE', 'PLANNED')
                AND ra.allocation_start_date <= :endDate
                AND ra.allocation_end_date >= :startDate
            WHERE r.active_flag = 1
            AND (:designation IS NULL OR r.designation = :designation)
            AND (:location IS NULL OR r.working_location = :location)
            AND (:employmentType IS NULL OR r.employment_type = :employmentType)
            AND (:minExp IS NULL OR r.experiance >= :minExp)
            AND (:maxExp IS NULL OR r.experiance <= :maxExp)
            GROUP BY r.resource_id
        ) resource_allocations
        """, nativeQuery = true)
    List<TimelineKpiProjection> getTimelineKPIWindow(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("designation") String designation,
        @Param("location") String location,
        @Param("employmentType") String employmentType,
        @Param("minExp") Integer minExp,
        @Param("maxExp") Integer maxExp
    );

    // Full History Mode KPI Query
    @Query(value = """
        SELECT
            COUNT(DISTINCT r.resource_id) as totalResources,
            SUM(CASE 
                WHEN resource_avg_allocation <= 20 THEN 1 ELSE 0 END) as fullyAvailable,
            SUM(CASE 
                WHEN resource_avg_allocation > 20 AND resource_avg_allocation <= 70 THEN 1 ELSE 0 END) as partiallyAvailable,
            SUM(CASE 
                WHEN resource_avg_allocation > 70 AND resource_avg_allocation <= 100 THEN 1 ELSE 0 END) as fullyAllocated,
            SUM(CASE 
                WHEN resource_avg_allocation > 100 THEN 1 ELSE 0 END) as overAllocated,
            COUNT(CASE 
                WHEN r.notice_start_date IS NOT NULL AND r.notice_end_date IS NOT NULL
                AND CURRENT_DATE BETWEEN r.notice_start_date AND r.notice_end_date 
                THEN 1 ELSE NULL END) as noticePeriodResources,
            COUNT(CASE 
                WHEN r.notice_start_date IS NOT NULL AND r.notice_end_date IS NOT NULL
                AND CURRENT_DATE BETWEEN r.notice_start_date AND r.notice_end_date
                AND resource_avg_allocation <= 50 
                THEN 1 ELSE NULL END) as availableNoticePeriodResources
        FROM (
            SELECT
                r.resource_id,
                COALESCE(
                    CASE 
                        WHEN SUM(DATEDIFF(ra.allocation_end_date, ra.allocation_start_date) + 1) = 0 THEN 0
                        ELSE SUM(
                            (DATEDIFF(ra.allocation_end_date, ra.allocation_start_date) + 1)
                            * ra.allocation_percentage
                        ) / SUM(DATEDIFF(ra.allocation_end_date, ra.allocation_start_date) + 1)
                    END,
                    0
                ) AS resource_avg_allocation
            FROM resource r
            LEFT JOIN resource_allocation ra
                ON ra.resource_id = r.resource_id
                AND ra.allocation_status IN ('ACTIVE', 'PLANNED')
            WHERE r.active_flag = 1
            AND (:designation IS NULL OR r.designation = :designation)
            AND (:location IS NULL OR r.working_location = :location)
            AND (:employmentType IS NULL OR r.employment_type = :employmentType)
            AND (:minExp IS NULL OR r.experiance >= :minExp)
            AND (:maxExp IS NULL OR r.experiance <= :maxExp)
            GROUP BY r.resource_id
        ) resource_allocations
        """, nativeQuery = true)
    List<TimelineKpiProjection> getTimelineKPIFullHistory(
        @Param("designation") String designation,
        @Param("location") String location,
        @Param("employmentType") String employmentType,
        @Param("minExp") Integer minExp,
        @Param("maxExp") Integer maxExp
    );
}
