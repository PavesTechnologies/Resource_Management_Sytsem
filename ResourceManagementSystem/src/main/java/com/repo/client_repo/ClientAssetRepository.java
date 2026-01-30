package com.repo.client_repo;

import com.entity.client_entities.ClientAsset;
import com.entity_enums.client_enums.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientAssetRepository extends JpaRepository<ClientAsset, Long> {
    List<ClientAsset> findByClient_ClientId(Long clientId);
    long countByStatus(AssetStatus status);

}
