package com.controller.client_controllers;


import com.dto.ApiResponse;
import com.dto.client_dto.SerialNumberDto;
import com.entity.client_entities.ClientAsset;
import com.service_imple.client_service_impl.ClientAssetServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/client-assets")
public class ClientAssetController {
    private final ClientAssetServiceImpl service;

    public ClientAssetController(ClientAssetServiceImpl service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createClientAsset(
            @RequestBody ClientAsset asset) {

        ApiResponse<String> response = service.createClientAsset(asset);

        return ResponseEntity
                .status(response.getSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                .body(response);
    }

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
     * @return ResponseEntity with creation result
     */
    @PostMapping(
        value = "/clients/{clientId}/assets",
        consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<String>> createClientAssetWithSerials(
            @PathVariable UUID clientId,
            @RequestParam("assetName") String assetName,
            @RequestParam("assetCategory") String assetCategory,
            @RequestParam("assetType") String assetType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("quantity") Integer quantity,
            @RequestPart(value = "serialFile", required = false) MultipartFile serialFile) {

        ApiResponse<String> response = service.createClientAssetWithSerials(
                clientId, assetName, assetCategory, assetType, description, quantity, serialFile);

        return ResponseEntity
                .status(response.getSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PutMapping("/{assetId}")
    public ResponseEntity<ApiResponse<String>> update(
            @PathVariable UUID assetId,
            @RequestBody ClientAsset asset) {

        ApiResponse<String> response = service.updateClientAsset(assetId, asset);
        return ResponseEntity
                .status(response.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @DeleteMapping("/{assetId}")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable UUID assetId) {

        ApiResponse<String> response = service.deleteClientAsset(assetId);
        return ResponseEntity
                .status(response.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // FETCH ENABLEMENTS BY CLIENT
    @GetMapping("/client/{clientId}")
    public ResponseEntity<ApiResponse<?>> getAssetsByClient(
            @PathVariable UUID clientId) {

        ApiResponse<?> response = service.getAssetsByClient(clientId);

        return ResponseEntity
                .status(response.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<ApiResponse<ClientAsset>> getAssetById(
            @PathVariable UUID assetId) {
        
        ApiResponse<ClientAsset> response = service.getAssetById(assetId);
        
        return ResponseEntity
                .status(response.getSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(response);
    }
//    @GetMapping("/dashboard")
//    public ResponseEntity<Map<String, Object>> dashboard() {
//        return ResponseEntity.ok(service.getAssetManagementDashboard());
//    }


//    @GetMapping("/utilization-percentage")
//    public ResponseEntity<Map<String, Object>> getAssetUtilizationPercentage() {
//        return ResponseEntity.ok(service.getAssetUtilizationPercentage());
//    }

//    @GetMapping("/dashboard")
//    public ResponseEntity<Map<String, Object>> getAssetManagementDashboard() {
//        return ResponseEntity.ok(service.getAssetManagementDashboard());
//    }
//
//    @GetMapping("/total-assets")
//    public ResponseEntity<Map<String, Object>> getTotalAssets() {
//        return ResponseEntity.ok(service.getTotalAssetsCount());
//    }
//
//    @GetMapping("/assigned-assets")
//    public ResponseEntity<Map<String, Object>> getAssignedAssets() {
//        return ResponseEntity.ok(service.getAssignedAssetsCount());
//    }
//
//    @GetMapping("/available-assets")
//    public ResponseEntity<Map<String, Object>> getAvailableAssets() {
//        return ResponseEntity.ok(service.getAvailableAssetsCount());
//    }

    @GetMapping("/dashboard/client/{clientId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardByClient(
            @PathVariable UUID clientId) {

        Map<String, Object> dashboard = service.getAssetDashboardByClient(clientId);
        ApiResponse<Map<String, Object>> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage("Client asset dashboard retrieved successfully");
        response.setData(dashboard);
        return ResponseEntity.ok(response);
    }

    /**
     * Get available (unassigned) serial numbers for an asset.
     * Used in Assign Asset modal dropdown.
     * 
     * @param assetId Asset UUID
     * @return List of available serial numbers as DTOs
     */
    @GetMapping("/{assetId}/available-serials")
    public ResponseEntity<List<SerialNumberDto>> getAvailableSerials(
            @PathVariable UUID assetId) {
        
        List<SerialNumberDto> availableSerials = service.getAvailableSerials(assetId);
        
        return ResponseEntity.ok(availableSerials);
    }

    /**
     * Add serial numbers to an existing asset from Excel file.
     * 
     * @param assetId Asset UUID
     * @param serialFile Excel file containing serial numbers
     * @return ApiResponse with result
     */
    @PostMapping(value = "/{assetId}/add-serials", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> addSerialNumbersToAsset(
            @PathVariable UUID assetId,
            @RequestPart("serialFile") MultipartFile serialFile) {
        
        ApiResponse<String> response = service.addSerialNumbersToAsset(assetId, serialFile);
        
        return ResponseEntity
                .status(response.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }


}
