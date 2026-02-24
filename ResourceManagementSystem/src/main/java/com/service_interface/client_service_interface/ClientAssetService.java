package com.service_interface.client_service_interface;

import com.dto.ApiResponse;
import com.dto.client_dto.SerialNumberDto;
import com.entity.client_entities.ClientAsset;
import com.entity.client_entities.ClientAssetSerial;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ClientAssetService {
    ApiResponse<String> createClientAsset (ClientAsset asset);
    
    /**
     * Create a new asset with optional serial numbers from Excel file.
     * 
     * @param clientId Client UUID
     * @param assetName Asset name
     * @param assetCategory Asset category
     * @param assetType Asset type
     * @param description Asset description (optional)
     * @param quantity Asset quantity
     * @param serialFile Excel file containing serial numbers (optional)
     * @return ApiResponse with creation result
     */
    ApiResponse<String> createClientAssetWithSerials(
            UUID clientId,
            String assetName,
            String assetCategory,
            String assetType,
            String description,
            Integer quantity,
            MultipartFile serialFile
    );
    
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

    /**
     * Get available (unassigned) serial numbers for an asset.
     * Used in Assign Asset modal dropdown.
     * 
     * @param assetId Asset UUID
     * @return List of available serial numbers as DTOs
     */
    List<SerialNumberDto> getAvailableSerials(UUID assetId);

    /**
     * Add serial numbers to an existing asset from Excel file.
     * 
     * @param assetId Asset UUID
     * @param serialFile Excel file containing serial numbers
     * @return ApiResponse with result
     */
    ApiResponse<String> addSerialNumbersToAsset(UUID assetId, MultipartFile serialFile);

}
