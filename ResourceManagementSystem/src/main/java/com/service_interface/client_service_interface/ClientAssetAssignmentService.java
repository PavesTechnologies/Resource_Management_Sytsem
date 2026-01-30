package com.service_interface.client_service_interface;

import com.dto.ApiResponse;
import com.entity.client_entities.ClientAssetAssignment;

import java.time.LocalDate;

public interface ClientAssetAssignmentService {
    ApiResponse<Void> assignAsset(Long assetId, ClientAssetAssignment assignment);

    ApiResponse<Void> updateAssignment(Long assignmentId, ClientAssetAssignment assignment);

    ApiResponse<Void> deleteAssignment(Long assignmentId);

    ApiResponse<?> getAllAssignments();
    
    ApiResponse<?> getAssignmentsByAssetId(Long assetId);

    ApiResponse<Void> returnAsset(
            Long assignmentId,
            LocalDate actualReturnDate,
            String remarks
    );
}
