package com.repo.report_repo;

import com.entity.resource_entities.Resource;
import com.entity_enums.centralised_enums.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface BenchPoolReportRepository extends JpaRepository<Resource, Long> {

    @Query(value = """
        SELECT 
            r.resourceId as resourceId,
            r.fullName as name,
            r.employmentStatus as status,
            r.workingLocation as region,
            r.designation as role,
            COALESCE(r.annualCtc, 0) as cost,
            ra.allocationEndDate as allocationEndDate,
            p.name as lastProject,
            c.clientName as client,
            s.name as skills,
            r.dateOfJoining as dateOfJoining
        FROM Resource r
        LEFT JOIN ResourceAllocation ra ON r.resourceId = ra.resource.resourceId
            AND ra.allocationEndDate = (
                SELECT MAX(ra2.allocationEndDate) 
                FROM ResourceAllocation ra2 
                WHERE ra2.resource.resourceId = r.resourceId
            )
        LEFT JOIN Project p ON ra.project.pmsProjectId = p.pmsProjectId
        LEFT JOIN Client c ON p.clientId = c.clientId
        LEFT JOIN ResourceSkill rs ON r.resourceId = rs.resourceId AND rs.activeFlag = true
        LEFT JOIN Skill s ON rs.skill.id = s.id
        WHERE r.employmentStatus IN ('ACTIVE', 'ON_NOTICE')
        AND r.allocationAllowed = true
        AND (
            ra.allocationId IS NULL 
            OR ra.allocationEndDate < CURRENT_DATE
            OR ra.allocationStatus != 'ACTIVE'
        )
        AND (:skill IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :skill, '%')))
        AND (:role IS NULL OR LOWER(r.designation) LIKE LOWER(CONCAT('%', :role, '%')))
        AND (:region IS NULL OR LOWER(r.workingLocation) LIKE LOWER(CONCAT('%', :region, '%')))
        AND (:maxCost IS NULL OR r.annualCtc <= :maxCost)
        AND (:minBenchDays IS NULL OR DATEDIFF(CURRENT_DATE, COALESCE(ra.allocationEndDate, r.dateOfJoining)) >= :minBenchDays)
        AND (:maxBenchDays IS NULL OR DATEDIFF(CURRENT_DATE, COALESCE(ra.allocationEndDate, r.dateOfJoining)) <= :maxBenchDays)
        AND (:riskLevel IS NULL)
        ORDER BY 
            CASE 
                WHEN DATEDIFF(CURRENT_DATE, COALESCE(ra.allocationEndDate, r.dateOfJoining)) > 30 THEN 1
                WHEN r.annualCtc > 80000 AND DATEDIFF(CURRENT_DATE, COALESCE(ra.allocationEndDate, r.dateOfJoining)) > 20 THEN 1
                WHEN DATEDIFF(CURRENT_DATE, COALESCE(ra.allocationEndDate, r.dateOfJoining)) > 15 THEN 2
                ELSE 3
            END,
            DATEDIFF(CURRENT_DATE, COALESCE(ra.allocationEndDate, r.dateOfJoining)) DESC
        """, 
        countQuery = """
        SELECT COUNT(DISTINCT r.resourceId)
        FROM Resource r
        LEFT JOIN ResourceAllocation ra ON r.resourceId = ra.resource.resourceId
            AND ra.allocationEndDate = (
                SELECT MAX(ra2.allocationEndDate) 
                FROM ResourceAllocation ra2 
                WHERE ra2.resource.resourceId = r.resourceId
            )
        LEFT JOIN Project p ON ra.project.pmsProjectId = p.pmsProjectId
        LEFT JOIN Client c ON p.clientId = c.clientId
        LEFT JOIN ResourceSkill rs ON r.resourceId = rs.resourceId AND rs.activeFlag = true
        LEFT JOIN Skill s ON rs.skill.id = s.id
        WHERE r.employmentStatus IN ('ACTIVE', 'ON_NOTICE')
        AND r.allocationAllowed = true
        AND (
            ra.allocationId IS NULL 
            OR ra.allocationEndDate < CURRENT_DATE
            OR ra.allocationStatus != 'ACTIVE'
        )
        AND (:skill IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :skill, '%')))
        AND (:role IS NULL OR LOWER(r.designation) LIKE LOWER(CONCAT('%', :role, '%')))
        AND (:region IS NULL OR LOWER(r.workingLocation) LIKE LOWER(CONCAT('%', :region, '%')))
        AND (:maxCost IS NULL OR r.annualCtc <= :maxCost)
        AND (:minBenchDays IS NULL OR DATEDIFF(CURRENT_DATE, COALESCE(ra.allocationEndDate, r.dateOfJoining)) >= :minBenchDays)
        AND (:maxBenchDays IS NULL OR DATEDIFF(CURRENT_DATE, COALESCE(ra.allocationEndDate, r.dateOfJoining)) <= :maxBenchDays)
        AND (:riskLevel IS NULL)
        """)
    Page<Object[]> findBenchPoolReportData(
            @Param("skill") String skill,
            @Param("role") String role,
            @Param("region") String region,
            @Param("maxCost") BigDecimal maxCost,
            @Param("minBenchDays") Long minBenchDays,
            @Param("maxBenchDays") Long maxBenchDays,
            @Param("riskLevel") RiskLevel riskLevel,
            Pageable pageable
    );
}
