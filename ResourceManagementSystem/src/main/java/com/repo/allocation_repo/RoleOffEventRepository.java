package com.repo.allocation_repo;

import com.entity.allocation_entities.RoleOffEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoleOffEventRepository extends JpaRepository<RoleOffEvent, UUID> {
    boolean existsByProject_PmsProjectIdAndResource_ResourceId(
            Long projectId,
            Long resourceId
    );
}
