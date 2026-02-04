package com.service_imple.client_service_impl;


import com.dto.ApiResponse;
import com.entity.client_entities.Client;
import com.entity.client_entities.ClientAsset;
import com.entity_enums.client_enums.AssetCategory;
import com.entity_enums.client_enums.AssetStatus;
import com.repo.client_repo.ClientAssetAssignmentRepo;
import com.repo.client_repo.ClientAssetRepository;
import com.repo.client_repo.ClientRepo;
import com.service_interface.client_service_interface.ClientAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientAssetServiceImpl implements ClientAssetService {

    private final ClientAssetRepository assetRepository;
    private final ClientRepo clientRepository;
    private final ClientAssetAssignmentRepo assignmentRepo;

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
    public ApiResponse<String> updateClientAsset(UUID assetId, ClientAsset asset) {
        try {
            ClientAsset existing = assetRepository.findById(assetId)
                    .orElseThrow(() -> new RuntimeException("Asset not found"));

            long assignedCount =
                    assignmentRepo.countByAsset_AssetIdAndActiveTrue(assetId);

            if (asset.getQuantity() < assignedCount) {
                return new ApiResponse<>(
                        false,
                        "Quantity cannot be reduced below assigned assets ("
                                + assignedCount + " already assigned).",
                        null
                );
            }

            existing.setAssetName(asset.getAssetName());
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
    public ApiResponse<String> deleteClientAsset(UUID assetId) {
        try {
            ClientAsset asset = assetRepository.findById(assetId)
                    .orElseThrow(() -> new RuntimeException("Asset not found"));

            long assignedCount =
                    assignmentRepo.countByAsset_AssetIdAndActiveTrue(assetId);

            if (assignedCount > 0) {
                return new ApiResponse<>(
                        false,
                        "Asset cannot be deleted because it has active assignments.",
                        null
                );
            }

            asset.setStatus(AssetStatus.INACTIVE);
            asset.setUpdatedAt(LocalDateTime.now());
            assetRepository.save(asset);

            return new ApiResponse<>(
                    true,
                    "Client asset deactivated successfully.",
                    null
            );

        } catch (Exception e) {
            return new ApiResponse<>(
                    false,
                    "Client asset deletion failed: " + e.getMessage(),
                    null
            );
        }
    }


    @Override
    public ApiResponse<?> getAssetsByClient(UUID clientId) {
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
    public ApiResponse<ClientAsset> getAssetById(UUID assetId) {
        try {
            ClientAsset asset = assetRepository.findById(assetId)
                    .orElseThrow(() -> new RuntimeException("Asset not found"));

            return new ApiResponse<>(
                    true,
                    "Asset fetched successfully",
                    asset
            );
        } catch (Exception e) {
            return new ApiResponse<>(
                    false,
                    "Failed to fetch asset: " + e.getMessage(),
                    null
            );
        }
    }


    @Override
    public Map<String, Object> getAssetManagementDashboard() {

        Map<String, Object> data = new HashMap<>();

        data.put("totalAssets",
                assetRepository.sumQuantity());

        data.put("assignedAssets",
                assignmentRepo.countActiveAssigned());

        long total = assetRepository.sumQuantity();
        long assigned = assignmentRepo.countActiveAssigned();

        data.put("availableAssets",
                Math.max(0, total - assigned));

        double utilization =
                total == 0 ? 0.0 : (assigned * 100.0) / total;

        data.put("utilizationPercentage",
                Math.round(utilization * 100.0) / 100.0);

        return Map.of(
                "success", true,
                "message", "Asset management dashboard data",
                "data", data
        );
    }


    @Override
    public Map<String, Object> getTotalAssetsCount() {
        long totalAssets = assetRepository.sumQuantity();
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

        Long total = assetRepository.sumQuantity();

        if (total == null || total == 0) {
            return Map.of(
                    "success", true,
                    "data", 0.0
            );
        }

        long assigned = assignmentRepo.countActiveAssigned();

        double utilization = (assigned * 100.0) / total;

        return Map.of(
                "success", true,
                "data", Math.round(utilization * 100.0) / 100.0
        );
    }
    @Override
    public Map<String, Object> getAssetDashboardByClient(UUID clientId) {

        long totalUnits =
                assetRepository.sumActiveQuantityByClient(clientId);

        long assignedUnits =
                assignmentRepo.countAssignedUnitsByClient(clientId);

        // 🚨 SAFETY: NEVER allow assigned > total
        if (assignedUnits > totalUnits) {
            assignedUnits = totalUnits;
        }

        long availableUnits = totalUnits - assignedUnits;

        double utilization =
                totalUnits == 0 ? 0.0 :
                        (assignedUnits * 100.0) / totalUnits;

        Map<String, Object> data = new HashMap<>();
        data.put("totalAssets", totalUnits);
        data.put("assignedAssets", assignedUnits);
        data.put("availableAssets", availableUnits);
        data.put(
                "utilizationPercentage",
                Math.round(utilization * 100.0) / 100.0
        );

        return Map.of(
                "success", true,
                "message", "Client asset dashboard data",
                "data", data
        );
    }

}
