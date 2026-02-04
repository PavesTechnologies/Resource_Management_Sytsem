package com.service_interface.client_service_interface;

import com.dto.ApiResponse;
import com.dto.AssetAssignmentKPIDTo;
import com.entity.client_entities.ClientAssetAssignment;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.UUID;

public interface ClientAssetAssignmentService {
    ResponseEntity<?> assignAsset(UUID assetId, ClientAssetAssignment assignment);

    ApiResponse<Void> updateAssignment(UUID assignmentId, ClientAssetAssignment assignment);

    ResponseEntity<?> deleteAssignment(UUID assignmentId);

    ApiResponse<?> getAllAssignments();
    
    ApiResponse<?> getAssignmentsByAssetId(UUID assetId);

    ApiResponse<Void> returnAsset(
            UUID assignmentId,
            LocalDate actualReturnDate,
            String remarks
    );

    ResponseEntity<ApiResponse<AssetAssignmentKPIDTo>> getKPI(UUID assetId);
}
