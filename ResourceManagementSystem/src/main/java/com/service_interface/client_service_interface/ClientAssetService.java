package com.service_interface.client_service_interface;

import com.dto.client_dto.ApiResponse;
import com.entity.client_entities.ClientAsset;

import java.util.Map;
import java.util.UUID;

public interface ClientAssetService {
    ApiResponse<String> createClientAsset (ClientAsset asset);
    ApiResponse<String> updateClientAsset(UUID assetId, ClientAsset asset);
    ApiResponse<String> deleteClientAsset(UUID assetId);
    ApiResponse<?> getAssetsByClient(UUID ClientId);
    ApiResponse<ClientAsset> getAssetById(UUID assetId);

    Map<String, Object> getAssetManagementDashboard();
    Map<String, Object> getTotalAssetsCount();
    Map<String, Object> getAssignedAssetsCount();
    Map<String, Object> getAvailableAssetsCount();
    Map<String, Object> getAssetUtilizationPercentage();
    Map<String, Object> getAssetDashboardByClient(UUID clientId);

}
