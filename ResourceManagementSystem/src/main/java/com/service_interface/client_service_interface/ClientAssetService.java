package com.service_interface.client_service_interface;

import com.dto.ApiResponse;
import com.entity.client_entities.ClientAsset;

import java.util.Map;

public interface ClientAssetService {
    ApiResponse<String> createClientAsset (ClientAsset asset);
    ApiResponse<String> updateClientAsset(Long assetId, ClientAsset asset);
    ApiResponse<String> deleteClientAsset(Long assetId);
    ApiResponse<?> getAssetsByClient(Long ClientId);
    ApiResponse<ClientAsset> getAssetById(Long assetId);

    Map<String, Object> getAssetManagementDashboard();
    Map<String, Object> getTotalAssetsCount();
    Map<String, Object> getAssignedAssetsCount();
    Map<String, Object> getAvailableAssetsCount();
    Map<String, Object> getAssetUtilizationPercentage();
    Map<String, Object> getAssetDashboardByClient(Long clientId);

}
