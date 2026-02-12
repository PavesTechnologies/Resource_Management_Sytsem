package com.repo.kpi_repo;

import com.projection.DashboardKpiProjection;
import com.entity.resource_entities.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface DashboardKpiRepository extends JpaRepository<Resource, Long> {

    @Query(nativeQuery = true, value = """
        WITH filtered_resources AS (
            SELECT r.resource_id, r.employment_type, r.working_location, r.experiance, r.active_flag
            FROM resource r
            WHERE r.active_flag = true
              AND (:role IS NULL OR LOWER(r.designation) LIKE LOWER(CONCAT('%', :role, '%')))
              AND (:location IS NULL OR LOWER(r.working_location) LIKE LOWER(CONCAT('%', :location, '%')))
              AND (:employmentType IS NULL OR r.employment_type = :employmentType)
              AND (:minExperience IS NULL OR r.experiance >= :minExperience)
              AND (:maxExperience IS NULL OR r.experiance <= :maxExperience)
        ),
        resource_allocations AS (
            SELECT 
                fr.resource_id,
                COALESCE(SUM(ra.allocation_percentage), 0) as total_allocation
            FROM filtered_resources fr
            LEFT JOIN resource_allocation ra 
                ON fr.resource_id = ra.resource_id
               AND ra.allocation_status IN ('PLANNED', 'ACTIVE')
               AND ra.allocation_start_date <= :toDate
               AND ra.allocation_end_date >= :fromDate
            GROUP BY fr.resource_id
        ),
        resource_upcoming_availability AS (
            SELECT 
                ra.resource_id,
                ra.total_allocation,
                CASE WHEN EXISTS (
                    SELECT 1 FROM resource_allocation ra_future
                    WHERE ra_future.resource_id = ra.resource_id
                      AND ra_future.allocation_status IN ('PLANNED', 'ACTIVE')
                      AND ra_future.allocation_start_date > :toDate
                      AND ra_future.allocation_start_date <= DATE_ADD(:toDate, INTERVAL 30 DAY)
                      AND ra_future.allocation_percentage < 100
                ) THEN 1 ELSE 0 END as upcoming_available
            FROM resource_allocations ra
        )
        SELECT 
            COUNT(*) as totalResources,
            SUM(CASE WHEN total_allocation <= 20 THEN 1 ELSE 0 END) as fullyAvailable,
            SUM(CASE WHEN total_allocation > 20 AND total_allocation <= 70 THEN 1 ELSE 0 END) as partiallyAvailable,
            SUM(CASE WHEN total_allocation > 70 AND total_allocation <= 100 THEN 1 ELSE 0 END) as fullyAllocated,
            SUM(CASE WHEN total_allocation > 100 THEN 1 ELSE 0 END) as overAllocated,
            SUM(upcoming_available) as upcomingAvailability,
            CASE 
                WHEN COUNT(*) = 0 THEN 0
                ELSE ROUND((SUM(CASE WHEN total_allocation <= 20 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)))
            END as benchCapacity,
            CASE 
                WHEN COUNT(*) = 0 THEN 0
                ELSE ROUND(AVG(total_allocation))
            END as utilization
        FROM resource_upcoming_availability
        """)
    DashboardKpiProjection calculateDashboardKpis(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("role") String role,
            @Param("location") String location,
            @Param("employmentType") String employmentType,
            @Param("minExperience") Integer minExperience,
            @Param("maxExperience") Integer maxExperience
    );
}
