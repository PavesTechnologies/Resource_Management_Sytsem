package com.repo.client_repo;

import com.entity.client_entities.ResourceEnablementAssignment;
import com.entity_enums.client_enums.EnablementAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResourceEnablementAssignmentRepository extends JpaRepository<ResourceEnablementAssignment, UUID> {
    List<ResourceEnablementAssignment> findByResourceId(Long resourceId);

    List<ResourceEnablementAssignment> findByStatus(EnablementAssignmentStatus status);
}
