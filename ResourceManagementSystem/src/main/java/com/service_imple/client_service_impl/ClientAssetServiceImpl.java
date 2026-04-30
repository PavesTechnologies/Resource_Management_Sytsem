package com.service_imple.client_service_impl;


import com.dto.centralised_dto.ApiResponse;
import com.dto.client_dto.SerialNumberDto;
import com.entity.client_entities.Client;
import com.entity.client_entities.ClientAsset;
import com.entity.client_entities.ClientAssetSerial;
import com.entity_enums.client_enums.AssetCategory;
import com.entity_enums.client_enums.AssetStatus;
import com.repo.client_repo.ClientAssetAssignmentRepo;
import com.repo.client_repo.ClientAssetRepository;
import com.repo.client_repo.ClientAssetSerialRepository;
import com.repo.client_repo.ClientRepo;
import com.service_interface.client_service_interface.ClientAssetService;
import com.util.ExcelSerialNumberParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientAssetServiceImpl implements ClientAssetService {

    private final ClientAssetRepository assetRepository;
    private final ClientRepo clientRepository;
    private final ClientAssetAssignmentRepo assignmentRepo;
    private final ClientAssetSerialRepository assetSerialRepository;

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
            String errorMessage = "Client asset creation failed";
            
            // Handle specific JPA/transaction errors
            if (e.getCause() != null && e.getCause().getMessage().contains("ConstraintViolationException")) {
                errorMessage = "Asset creation failed: Invalid data provided";
            } else if (e.getMessage() != null && e.getMessage().contains("could not execute statement")) {
                errorMessage = "Asset creation failed: Database error occurred";
            } else if (e.getMessage() != null && e.getMessage().contains("Could not commit JPA transaction")) {
                errorMessage = "Asset creation failed: Unable to save asset. Please try again.";
            }
            
            return new ApiResponse<>(
                    false,
                    errorMessage,
                    null
            );
        }
    }

    @Override
    @Transactional
    public ApiResponse<String> createClientAssetWithSerials(
            UUID clientId,
            String assetName,
            String assetCategory,
            String assetType,
            String description,
            Integer quantity,
            MultipartFile serialFile) {

        try {
            // ========================================================================
            // INPUT VALIDATION
            // ========================================================================
            validateAssetInputs(assetName, assetCategory, assetType, quantity);

            // Verify client exists
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Client not found"));

            // ========================================================================
            // SERIAL NUMBER PROCESSING
            // ========================================================================
            List<String> serialNumbers = new ArrayList<>();

            if (serialFile != null && !serialFile.isEmpty()) {
                
                // Parse Excel file
                serialNumbers = ExcelSerialNumberParser.parseSerialNumbers(serialFile);
                
                // Validate serial count vs quantity
                if (serialNumbers.size() != quantity) {
                    throw new RuntimeException("Serial count must match quantity. Expected: " + 
                            quantity + ", Found: " + serialNumbers.size());
                }

                // Check for duplicates within Excel file
                List<String> duplicatesInFile = findDuplicates(serialNumbers);
                if (!duplicatesInFile.isEmpty()) {
                    throw new RuntimeException("Duplicate serial numbers found in Excel file: " + 
                            String.join(", ", duplicatesInFile));
                }

                // Check for existing serial numbers in database
                List<String> existingSerials = assetSerialRepository.findExistingSerialNumbers(serialNumbers);
                if (!existingSerials.isEmpty()) {
                    throw new RuntimeException("Serial number(s) already exist in database: " + 
                            String.join(", ", existingSerials));
                }

            }

            // ========================================================================
            // ASSET CREATION
            // ========================================================================
            ClientAsset asset = ClientAsset.builder()
                    .client(client)
                    .assetName(assetName)
                    .assetCategory(AssetCategory.valueOf(assetCategory.toUpperCase()))
                    .assetType(assetType)
                    .description(description)
                    .quantity(quantity)
                    .status(AssetStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();

            ClientAsset savedAsset = assetRepository.save(asset);
            log.info("Created asset: {} for client: {}", assetName, clientId);

            // ========================================================================
            // SERIAL NUMBER CREATION (if applicable)
            // ========================================================================
            if (!serialNumbers.isEmpty()) {
                List<ClientAssetSerial> assetSerials = serialNumbers.stream()
                        .map(serial -> ClientAssetSerial.builder()
                                .asset(savedAsset)
                                .serialNumber(serial)
                                .status(AssetStatus.ACTIVE)
                                .build())
                        .collect(Collectors.toList());

                assetSerialRepository.saveAll(assetSerials);
                log.info("Created {} serial numbers for asset: {}", serialNumbers.size(), savedAsset.getAssetId());
            }

            String successMessage = String.format("Asset '%s' created successfully with %d units", 
                    assetName, quantity);

            if (!serialNumbers.isEmpty()) {
                successMessage += String.format(" and %d serial numbers", serialNumbers.size());
            }

            return new ApiResponse<>(true, successMessage, null);

        } catch (Exception e) {
            log.error("Asset creation with serials failed: {}", e.getMessage(), e);
            return new ApiResponse<>(false, "Asset creation failed: " + e.getMessage(), null);
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

            // Check if asset has serial numbers uploaded from Excel
            long serialCount = assetSerialRepository.countByAsset_AssetId(assetId);
            
            if (serialCount > 0) {
                // Asset has serial numbers - quantity must match exactly
                if (asset.getQuantity() != serialCount) {
                    return new ApiResponse<>(
                            false,
                            "Cannot change quantity. Asset has " + serialCount + 
                                    " serial numbers from Excel upload. Quantity must remain " + serialCount + ".",
                            null
                    );
                }
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
            String errorMessage = "Client asset update failed";
            
            // Handle specific JPA/transaction errors
            if (e.getCause() != null && e.getCause().getMessage().contains("ConstraintViolationException")) {
                errorMessage = "Asset update failed: Invalid data provided";
            } else if (e.getMessage() != null && e.getMessage().contains("could not execute statement")) {
                errorMessage = "Asset update failed: Database error occurred";
            } else if (e.getMessage() != null && e.getMessage().contains("Could not commit JPA transaction")) {
                errorMessage = "Asset update failed: Unable to update asset. Please try again.";
            }
            
            return new ApiResponse<>(
                    false,
                    errorMessage,
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
            String errorMessage = "Client asset deletion failed";
            
            // Handle specific JPA/transaction errors
            if (e.getCause() != null && e.getCause().getMessage().contains("ConstraintViolationException")) {
                errorMessage = "Asset deletion failed: Invalid data provided";
            } else if (e.getMessage() != null && e.getMessage().contains("could not execute statement")) {
                errorMessage = "Asset deletion failed: Database error occurred";
            } else if (e.getMessage() != null && e.getMessage().contains("Could not commit JPA transaction")) {
                errorMessage = "Asset deletion failed: Unable to delete asset. Please try again.";
            }
            
            return new ApiResponse<>(
                    false,
                    errorMessage,
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

    /**
     * Validate basic asset input parameters.
     * 
     * @param assetName Asset name
     * @param assetCategory Asset category
     * @param assetType Asset type
     * @param quantity Asset quantity
     * @throws RuntimeException If validation fails
     */
    private void validateAssetInputs(String assetName, String assetCategory, String assetType, Integer quantity) 
            throws RuntimeException {
        
        if (assetName == null || assetName.trim().isEmpty()) {
            throw new RuntimeException("Asset name is required");
        }

        if (assetCategory == null || assetCategory.trim().isEmpty()) {
            throw new RuntimeException("Asset category is required");
        }

        if (assetType == null || assetType.trim().isEmpty()) {
            throw new RuntimeException("Asset type is required");
        }

        if (quantity == null || quantity < 1) {
            throw new RuntimeException("Quantity must be at least 1");
        }

        if (quantity > 10000) {
            throw new RuntimeException("Quantity cannot exceed 10,000");
        }
    }

    /**
     * Find duplicate serial numbers in a list.
     * 
     * @param serialNumbers List of serial numbers to check
     * @return List of duplicate serial numbers
     */
    private List<String> findDuplicates(List<String> serialNumbers) {
        return serialNumbers.stream()
                .filter(serial -> serialNumbers.stream()
                        .filter(s -> s.equals(serial))
                        .count() > 1)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<SerialNumberDto> getAvailableSerials(UUID assetId) {
        log.info("Fetching available serial numbers for asset: {}", assetId);
        
        // First verify the asset exists
        if (!assetRepository.existsById(assetId)) {
            log.warn("Asset not found: {}", assetId);
            return new ArrayList<>();
        }
        
        List<ClientAssetSerial> availableSerials = assetSerialRepository.findAvailableSerialsByAssetId(assetId);
        
        log.info("Found {} available serial numbers for asset: {}", availableSerials.size(), assetId);
        
        List<SerialNumberDto> serialDtos = availableSerials.stream()
                .map(SerialNumberDto::fromEntity)
                .collect(Collectors.toList());
        
        return serialDtos;
    }

    @Override
    @Transactional
    public ApiResponse<String> addSerialNumbersToAsset(UUID assetId, MultipartFile serialFile) {
        try {
            // Verify asset exists
            ClientAsset asset = assetRepository.findById(assetId)
                    .orElseThrow(() -> new RuntimeException("Asset not found"));

            // Parse Excel file
            List<String> serialNumbers = ExcelSerialNumberParser.parseSerialNumbers(serialFile);
            
            if (serialNumbers.isEmpty()) {
                throw new RuntimeException("No serial numbers found in Excel file");
            }

            // Check for existing serial numbers in database
            List<String> existingSerials = assetSerialRepository.findExistingSerialNumbers(serialNumbers);
            if (!existingSerials.isEmpty()) {
                throw new RuntimeException("Serial number(s) already exist in database: " + 
                        String.join(", ", existingSerials));
            }

            // Check for duplicates within Excel file
            List<String> duplicatesInFile = findDuplicates(serialNumbers);
            if (!duplicatesInFile.isEmpty()) {
                throw new RuntimeException("Duplicate serial numbers found in Excel file: " + 
                        String.join(", ", duplicatesInFile));
            }

            // Create serial numbers
            List<ClientAssetSerial> assetSerials = serialNumbers.stream()
                    .map(serial -> ClientAssetSerial.builder()
                            .asset(asset)
                            .serialNumber(serial)
                            .status(AssetStatus.ACTIVE)
                            .assigned(false)
                            .build())
                    .collect(Collectors.toList());

            assetSerialRepository.saveAll(assetSerials);
            
            log.info("Added {} serial numbers to asset: {}", serialNumbers.size(), assetId);

            return new ApiResponse<>(true, 
                    String.format("Successfully added %d serial numbers to asset '%s'", 
                            serialNumbers.size(), asset.getAssetName()), 
                    null);

        } catch (Exception e) {
            log.error("Failed to add serial numbers to asset: {}", e.getMessage(), e);
            return new ApiResponse<>(false, "Failed to add serial numbers: " + e.getMessage(), null);
        }
    }
}
