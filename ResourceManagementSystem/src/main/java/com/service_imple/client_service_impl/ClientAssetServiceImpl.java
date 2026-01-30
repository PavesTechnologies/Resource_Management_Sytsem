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
import java.util.HashMap;
import java.util.Map;

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

            asset.setClient(client);
            asset.setStatus(AssetStatus.ACTIVE);
            asset.setCreatedAt(LocalDateTime.now());

            assetRepository.save(asset);

            return new ApiResponse<>(true,
                    "Client asset created successfully.",
                    null);

        } catch (Exception e) {
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

            existing.setAssetName(asset.getAssetName());
            existing.setAssetCategory(asset.getAssetCategory());
            existing.setAssetType(asset.getAssetType());
            existing.setQuantity(asset.getQuantity());
            existing.setUpdatedAt(LocalDateTime.now());

            assetRepository.save(existing);

            return new ApiResponse<>(true,
                    "Client asset updated successfully.",
                    null);

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

    @Override
    public Map<String, Object> getAssetManagementDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalAssets", getTotalAssetsCount().get("data"));
        dashboard.put("assignedAssets", getAssignedAssetsCount().get("data"));
        dashboard.put("availableAssets", getAvailableAssetsCount().get("data"));
        dashboard.put("utilizationPercentage", getAssetUtilizationPercentage().get("data"));
        return Map.of("success", true, "message", "Asset management dashboard data", "data", dashboard);
    }

    @Override
    public Map<String, Object> getTotalAssetsCount() {
        long totalAssets = assetRepository.count();
        return Map.of("success", true, "message", "Total assets count", "data", totalAssets);
    }

    @Override
    public Map<String, Object> getAssignedAssetsCount() {
        long assignedAssets = assetRepository.countByStatus(AssetStatus.ACTIVE);
        return Map.of("success", true, "message", "Assigned assets count", "data", assignedAssets);
    }

    @Override
    public Map<String, Object> getAvailableAssetsCount() {
        long totalAssets = assetRepository.count();
        long assignedAssets = assetRepository.countByStatus(AssetStatus.ACTIVE);
        long availableAssets = totalAssets - assignedAssets;
        return Map.of("success", true, "message", "Available assets count", "data", availableAssets);
    }

    @Override
    public Map<String, Object> getAssetUtilizationPercentage() {
        long totalAssets = assetRepository.count();
        if (totalAssets == 0) {
            return Map.of("success", true, "message", "Utilization percentage", "data", 0.0);
        }
        long assignedAssets = assetRepository.countByStatus(AssetStatus.ACTIVE);
        double utilization = (assignedAssets * 100.0) / totalAssets;
        return Map.of("success", true, "message", "Asset utilization percentage", "data", Math.round(utilization * 100.0) / 100.0);
    }


}
