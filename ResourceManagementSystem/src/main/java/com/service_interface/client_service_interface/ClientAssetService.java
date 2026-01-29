package com.service_interface.client_service_interface;

import com.dto.ApiResponse;
import com.entity.client_entities.ClientAsset;

public interface ClientAssetService {
    ApiResponse<String> createClientAsset (ClientAsset asset);
    ApiResponse<String> updateClientAsset(Long assetId, ClientAsset asset);
    ApiResponse<String> deleteClientAsset(Long assetId);
    ApiResponse<?> getAssetsByClient(Long ClientId);
}
