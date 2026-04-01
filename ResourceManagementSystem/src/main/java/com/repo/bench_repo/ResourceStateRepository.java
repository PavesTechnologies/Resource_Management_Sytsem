package com.repo.bench_repo;

import com.entity.bench.ResourceState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResourceStateRepository extends JpaRepository<ResourceState, Long>{
    Optional<ResourceState> findByResourceIdAndCurrentFlagTrue(Long resourceId);
}
