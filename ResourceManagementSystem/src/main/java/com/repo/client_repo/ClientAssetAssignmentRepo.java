package com.repo.client_repo;

import com.entity.client_entities.ClientAssetAssignment;
import com.entity_enums.client_enums.EnablementAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientAssetAssignmentRepo extends JpaRepository<ClientAssetAssignment, UUID> {
    List<ClientAssetAssignment> findByActiveTrue();

    boolean existsBySerialNumber(String serialNumber);

    boolean existsBySerialNumberAndAssignmentStatus(String serialNumber, EnablementAssignmentStatus assignmentStatus);
    long countByAsset_AssetIdAndAssignmentStatus(UUID assetId, EnablementAssignmentStatus assignmentStatus);

    boolean existsBySerialNumberAndAssignmentIdNot(
            String serialNumber,
            UUID assignmentId
    );
    
    Optional<List<ClientAssetAssignment>> findByAsset_AssetId(UUID assetId);

    long countByAsset_AssetIdAndActiveTrue(UUID assetId);

    @Query("""
            SELECT COUNT(a)
            FROM ClientAssetAssignment a
            WHERE a.active = true
            AND a.assignmentStatus IN ('ASSIGNED','IN_USE')
            """)
    long countActiveAssigned();

    @Query("""
    SELECT COUNT(a)
    FROM ClientAssetAssignment a
    WHERE a.asset.client.clientId = :clientId
    AND a.active = true
    AND a.assignmentStatus IN ('ASSIGNED','IN_USE')
""")
    long countActiveAssignedByClient(@Param("clientId")UUID clientId);

    @Query("""
    SELECT COUNT(a)
    FROM ClientAssetAssignment a
    WHERE a.asset.client.clientId = :clientId
    AND a.active = true
    AND a.assignmentStatus IN ('ASSIGNED','IN_USE')
""")
    long countAssignedUnitsByClient(UUID clientId);


}
