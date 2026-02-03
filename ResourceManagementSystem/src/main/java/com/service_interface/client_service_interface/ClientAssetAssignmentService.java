package com.service_interface.client_service_interface;

import com.dto.ApiResponse;
import com.entity.client_entities.ClientAssetAssignment;

import java.time.LocalDate;
import java.util.UUID;

public interface ClientAssetAssignmentService {
    ApiResponse<Void> assignAsset(UUID assetId, ClientAssetAssignment assignment);

    ApiResponse<Void> updateAssignment(UUID assignmentId, ClientAssetAssignment assignment);

    ApiResponse<Void> deleteAssignment(UUID assignmentId);

    ApiResponse<?> getAllAssignments();
    
    ApiResponse<?> getAssignmentsByAssetId(UUID assetId);

    ApiResponse<Void> returnAsset(
            UUID assignmentId,
            LocalDate actualReturnDate,
            String remarks
    );
}
