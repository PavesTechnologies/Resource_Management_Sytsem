package com.repo.client_repo;

import com.entity.client_entities.ClientAssetSerial;
import com.entity_enums.client_enums.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for ClientAssetSerial entity operations.
 * Provides methods for serial number CRUD and validation queries.
 */
public interface ClientAssetSerialRepository extends JpaRepository<ClientAssetSerial, UUID> {

    /**
     * Find all serial numbers belonging to a specific asset.
     * 
     * @param assetId Asset UUID
     * @return List of serial numbers for asset
     */
    List<ClientAssetSerial> findByAsset_AssetId(UUID assetId);

    /**
     * Find all serial numbers belonging to a specific asset with ACTIVE status.
     * 
     * @param assetId Asset UUID
     * @return List of active serial numbers for asset
     */
    List<ClientAssetSerial> findByAsset_AssetIdAndStatus(UUID assetId, AssetStatus status);

    /**
     * Check if serial number exists in the database.
     * 
     * @param serialNumber Serial number to check
     * @return true if serial number exists
     */
    boolean existsBySerialNumber(String serialNumber);

    /**
     * Check if any of the provided serial numbers exist in the database.
     * Used for batch validation before creating serial numbers.
     * 
     * @param serialNumbers List of serial numbers to check
     * @return true if any serial number already exists
     */
    @Query("SELECT CASE WHEN COUNT(cas) > 0 THEN true ELSE false END FROM ClientAssetSerial cas WHERE cas.serialNumber IN :serialNumbers")
    boolean existsBySerialNumberIn(@Param("serialNumbers") List<String> serialNumbers);

    /**
     * Find serial numbers that already exist from the provided list.
     * Returns existing serial numbers for duplicate validation.
     * 
     * @param serialNumbers List of serial numbers to check
     * @return List of existing serial numbers
     */
    @Query("SELECT cas.serialNumber FROM ClientAssetSerial cas WHERE cas.serialNumber IN :serialNumbers")
    List<String> findExistingSerialNumbers(@Param("serialNumbers") List<String> serialNumbers);

    /**
     * Count serial numbers for a specific asset.
     * 
     * @param assetId Asset UUID
     * @return Count of serial numbers for the asset
     */
    long countByAsset_AssetId(UUID assetId);

    /**
     * Delete all serial numbers belonging to an asset.
     * Used when an asset is deleted.
     * 
     * @param assetId Asset UUID
     */
    void deleteByAsset_AssetId(UUID assetId);

    /**
     * Find all available (unassigned) serial numbers for a specific asset.
     * Used in Assign Asset modal dropdown.
     * 
     * @param assetId Asset UUID
     * @return List of available serial numbers
     */
    @Query("SELECT cas FROM ClientAssetSerial cas WHERE cas.asset.assetId = :assetId AND cas.assigned = false AND cas.status = 'ACTIVE'")
    List<ClientAssetSerial> findAvailableSerialsByAssetId(@Param("assetId") UUID assetId);

    /**
     * Find serial number by serial number string and check if it's assigned.
     * Used for validation during asset assignment.
     * 
     * @param serialNumber Serial number string
     * @return ClientAssetSerial if found
     */
    ClientAssetSerial findBySerialNumber(String serialNumber);
}
