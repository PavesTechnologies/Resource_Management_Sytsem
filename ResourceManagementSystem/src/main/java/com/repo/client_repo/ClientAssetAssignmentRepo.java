package com.repo.client_repo;

import com.entity.client_entities.ClientAssetAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientAssetAssignmentRepo extends JpaRepository<ClientAssetAssignment, Long> {
    List<ClientAssetAssignment> findByActiveTrue();
}
