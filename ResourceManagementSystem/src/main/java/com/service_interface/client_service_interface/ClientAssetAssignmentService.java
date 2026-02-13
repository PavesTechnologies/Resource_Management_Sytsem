package com.service_interface.client_service_interface;

import com.dto.ApiResponse;
import com.dto.client_dto.AssetAssignmentKPIDTo;
import com.entity.client_entities.ClientAssetAssignment;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.UUID;

public interface ClientAssetAssignmentService {
    ResponseEntity<ApiResponse<?>> assignAsset(UUID assetId, ClientAssetAssignment assignment);

    ResponseEntity<ApiResponse<?>> updateAssignment(UUID assignmentId, ClientAssetAssignment assignment);

    ResponseEntity<ApiResponse<?>> deleteAssignment(UUID assignmentId);

    ResponseEntity<ApiResponse<?>> getAllAssignments();
    
    ResponseEntity<ApiResponse<?>> getAssignmentsByAssetId(UUID assetId);

    ResponseEntity<ApiResponse<?>> returnAsset(
            UUID assignmentId,
            LocalDate actualReturnDate,
            String remarks
    );

    ResponseEntity<ApiResponse<AssetAssignmentKPIDTo>> getKPI(UUID assetId);

    ApiResponse<?> getAssignmentsByProjectId(Long projectId);
}
