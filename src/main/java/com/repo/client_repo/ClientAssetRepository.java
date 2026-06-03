package com.repo.client_repo;

import com.entity.client_entities.ClientAsset;
import com.entity_enums.client_enums.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ClientAssetRepository extends JpaRepository<ClientAsset, UUID> {
    List<ClientAsset> findByClient_ClientId(UUID clientId);

    long countByStatus(AssetStatus status);

//    long countByClient_ClientIdAndStatus(Long clientId, AssetStatus status);

    @Query("SELECT COALESCE(SUM(a.quantity), 0) FROM ClientAsset a WHERE a.status='ACTIVE'")
    Long sumQuantity();

    @Query("""
    SELECT COALESCE(SUM(a.quantity), 0)
    FROM ClientAsset a
    WHERE a.client.clientId = :clientId
    AND a.status = 'ACTIVE'
""")
    Long sumQuantityByClient(@Param("clientId")UUID clientId);

    @Query("""
    SELECT COALESCE(SUM(a.quantity), 0)
    FROM ClientAsset a
    WHERE a.client.clientId = :clientId
    AND a.status = 'ACTIVE'
""")
    Long sumActiveQuantityByClient(UUID clientId);



}
