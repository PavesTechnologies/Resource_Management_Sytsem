package com.repo.bench_repo;

import com.entity.bench.ResourceCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ResourceCostRepository extends JpaRepository<ResourceCost, Long> {
    @Query("""
        SELECT rc FROM ResourceCost rc
        WHERE rc.resourceId = :resourceId
        AND rc.effectiveFrom <= :today
        AND (rc.effectiveTo IS NULL OR rc.effectiveTo >= :today)
        """)
    Optional<ResourceCost> findActiveCost(Long resourceId, LocalDate today);
}
