package com.repo.bench_repo;

import com.entity.bench.ResourceState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResourceStateRepository extends JpaRepository<ResourceState, Long>{
    @Query("SELECT rs FROM ResourceState rs WHERE rs.resourceId = :resourceId AND rs.currentFlag = true ORDER BY rs.effectiveFrom DESC LIMIT 1")
    Optional<ResourceState> findByResourceIdAndCurrentFlagTrue(@Param("resourceId") String resourceId);
}
