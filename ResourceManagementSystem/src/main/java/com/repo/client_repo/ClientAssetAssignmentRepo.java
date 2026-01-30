package com.repo.client_repo;

import com.entity.client_entities.ClientAssetAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientAssetAssignmentRepo extends JpaRepository<ClientAssetAssignment, Long> {
    List<ClientAssetAssignment> findByActiveTrue();

    boolean existsBySerialNumber(String serialNumber);

    boolean existsBySerialNumberAndAssignmentIdNot(
            String serialNumber,
            Long assignmentId
    );
    
    Optional<List<ClientAssetAssignment>> findByAsset_AssetId(Long assetId);
}
