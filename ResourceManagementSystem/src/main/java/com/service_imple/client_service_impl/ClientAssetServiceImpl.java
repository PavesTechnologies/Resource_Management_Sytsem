package com.service_imple.client_service_impl;


import com.dto.ApiResponse;
import com.entity.client_entities.Client;
import com.entity.client_entities.ClientAsset;
import com.entity_enums.client_enums.AssetCategory;
import com.entity_enums.client_enums.AssetStatus;
import com.repo.client_repo.ClientAssetRepository;
import com.repo.client_repo.ClientRepo;
import com.service_interface.client_service_interface.ClientAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClientAssetServiceImpl implements ClientAssetService {

    private final ClientAssetRepository assetRepository;
    private final ClientRepo clientRepository;

    @Override
    public ApiResponse<String> createClientAsset(ClientAsset asset) {
        try {
            Client client = clientRepository.findById(
                    asset.getClient().getClientId()
            ).orElseThrow(() -> new RuntimeException("Client not found"));

            if (asset.getAssetCategory() == AssetCategory.DEVICE) {
                if (asset.getSerialNumber() == null || asset.getSerialNumber().isBlank()) {
                    throw new RuntimeException("Serial number is required for device assets");
                }

                asset.setSerialNumber(asset.getSerialNumber().trim().toUpperCase());
            }

            asset.setClient(client);
            asset.setStatus(AssetStatus.ACTIVE);
            asset.setCreatedAt(LocalDateTime.now());

            assetRepository.save(asset);

            return new ApiResponse<>(true, "Client asset created successfully.", null);

        }
        catch (DataIntegrityViolationException e) {
            // 🔴 UNIQUE constraint violation
            return new ApiResponse<>(
                    false,
                    "Serial number already exists",
                    null
            );
        }
        catch (Exception e) {
            return new ApiResponse<>(
                    false,
                    "Client asset creation failed: " + e.getMessage(),
                    null
            );
        }
    }

    @Override
    public ApiResponse<String> updateClientAsset(Long assetId, ClientAsset asset) {
        try {
            ClientAsset existing = assetRepository.findById(assetId)
                    .orElseThrow(() -> new RuntimeException("Asset not found"));

            if (existing.getAssetCategory() == AssetCategory.DEVICE
                    && asset.getSerialNumber() != null) {

                // 🔴 UNIQUE SERIAL CHECK (exclude same asset)
//                if (assetRepository.existsBySerialNumberAndAssetIdNot(
//                        asset.getSerialNumber(), assetId)) {
//                    throw new RuntimeException("Serial number already exists");
//                }

                existing.setSerialNumber(asset.getSerialNumber());
            }

            existing.setAssetName(asset.getAssetName());
            existing.setDescription(asset.getDescription());
            existing.setAssetCategory(asset.getAssetCategory());
            existing.setAssetType(asset.getAssetType());
            existing.setQuantity(asset.getQuantity());
            existing.setUpdatedAt(LocalDateTime.now());

            assetRepository.save(existing);

            return new ApiResponse<>(
                    true,
                    "Client asset updated successfully.",
                    null
            );

        } catch (Exception e) {
            return new ApiResponse<>(
                    false,
                    "Client asset update failed: " + e.getMessage(),
                    null
            );
        }
    }


    // SOFT DELETE (DEACTIVATE)
    @Override
    public ApiResponse<String> deleteClientAsset(Long assetId) {
        try {
            ClientAsset asset = assetRepository.findById(assetId)
                    .orElseThrow(() -> new RuntimeException("Asset not found"));

            asset.setStatus(AssetStatus.INACTIVE);
            asset.setUpdatedAt(LocalDateTime.now());

            assetRepository.save(asset);

            return new ApiResponse<>(true,
                    "Client asset deactivated successfully.",
                    null);

        } catch (Exception e) {
            return new ApiResponse<>(false,
                    "Client asset deletion failed: " + e.getMessage(),
                    null);
        }
    }

    @Override
    public ApiResponse<?> getAssetsByClient(Long clientId) {
        try {
            return new ApiResponse<>(
                    true,
                    "Client assets fetched successfully.",
                    assetRepository.findByClient_ClientId(clientId)
            );
        } catch (Exception e) {
            return new ApiResponse<>(
                    false,
                    "Failed to fetch client assets: " + e.getMessage(),
                    null
            );
        }
    }
}
