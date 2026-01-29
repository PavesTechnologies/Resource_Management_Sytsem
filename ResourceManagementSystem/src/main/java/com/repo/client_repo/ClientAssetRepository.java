package com.repo.client_repo;

import com.entity.client_entities.ClientAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientAssetRepository extends JpaRepository<ClientAsset, Long> {
    List<ClientAsset> findByClient_ClientId(Long clientId);

    boolean existsBySerialNumber(String serialNumber);

    boolean existsBySerialNumberAndAssetIdNot(String serialNumber, Long assetId);
}
