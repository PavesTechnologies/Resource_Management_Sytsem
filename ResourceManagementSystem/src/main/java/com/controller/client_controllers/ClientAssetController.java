package com.controller.client_controllers;


import com.dto.ApiResponse;
import com.entity.client_entities.ClientAsset;
import com.entity.client_entities.ClientAssetAssignment;
import com.repo.client_repo.ClientAssetAssignmentRepo;
import com.service_imple.client_service_impl.ClientAssetServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

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
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<String>> createClientAsset(
            @RequestBody ClientAsset asset) {

        ApiResponse<String> response = service.createClientAsset(asset);

        return ResponseEntity
                .status(response.getSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PutMapping("/{assetId}")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<String>> update(
            @PathVariable UUID assetId,
            @RequestBody ClientAsset asset) {

        ApiResponse<String> response = service.updateClientAsset(assetId, asset);
        return ResponseEntity
                .status(response.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @DeleteMapping("/{assetId}")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable UUID assetId) {

        ApiResponse<String> response = service.deleteClientAsset(assetId);
        return ResponseEntity
                .status(response.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // FETCH ENABLEMENTS BY CLIENT
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<?>> getAssetsByClient(
            @PathVariable UUID clientId) {

        ApiResponse<?> response = service.getAssetsByClient(clientId);

        return ResponseEntity
                .status(response.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @GetMapping("/{assetId}")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
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


    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<Map<String, Object>> getAssetManagementDashboard() {
        return ResponseEntity.ok(service.getAssetManagementDashboard());
    }

    @GetMapping("/total-assets")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<Map<String, Object>> getTotalAssets() {
        return ResponseEntity.ok(service.getTotalAssetsCount());
    }

    @GetMapping("/assigned-assets")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<Map<String, Object>> getAssignedAssets() {
        return ResponseEntity.ok(service.getAssignedAssetsCount());
    }

    @GetMapping("/available-assets")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<Map<String, Object>> getAvailableAssets() {
        return ResponseEntity.ok(service.getAvailableAssetsCount());
    }

    @GetMapping("/utilization-percentage")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<Map<String, Object>> getAssetUtilizationPercentage() {
        return ResponseEntity.ok(service.getAssetUtilizationPercentage());
    }

    @GetMapping("/dashboard/client/{clientId}")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<Map<String, Object>> getDashboardByClient(
            @PathVariable UUID clientId) {

        return ResponseEntity.ok(
                service.getAssetDashboardByClient(clientId)
        );
    }
}
