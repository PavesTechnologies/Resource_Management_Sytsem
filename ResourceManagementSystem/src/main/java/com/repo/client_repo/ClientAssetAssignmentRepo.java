package com.repo.client_repo;

import com.entity.client_entities.ClientAssetAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    long countByAsset_AssetIdAndActiveTrue(Long assetId);

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
    long countActiveAssignedByClient(@Param("clientId")Long clientId);

    @Query("""
    SELECT COUNT(a)
    FROM ClientAssetAssignment a
    WHERE a.asset.client.clientId = :clientId
    AND a.active = true
    AND a.assignmentStatus IN ('ASSIGNED','IN_USE')
""")
    long countAssignedUnitsByClient(Long clientId);


}
