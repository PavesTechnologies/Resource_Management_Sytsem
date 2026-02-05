package com.service_imple.client_service_impl;

import com.dto.ApiResponse;
import com.dto.client_dto.AssetAssignmentDTO;
import com.dto.client_dto.AssetAssignmentKPIDTo;
import com.dto.client_dto.AssetResponseDTO;
import com.entity.client_entities.ClientAsset;
import com.entity.client_entities.ClientAssetAssignment;
import com.entity_enums.client_enums.EnablementAssignmentStatus;
import com.global_exception_handler.ClientException;
import com.repo.client_repo.ClientAssetAssignmentRepo;
import com.repo.client_repo.ClientAssetRepository;
import com.repo.client_repo.ClientRepo;
import com.service_interface.client_service_interface.ClientAssetAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientAssetAssignmentServiceImpl implements ClientAssetAssignmentService {
    private final ClientAssetRepository assetRepository;
    private final ClientAssetAssignmentRepo assignmentRepository;
    private final ClientRepo clientRepo;
    // ASSIGN ASSET
    @Override
    public ResponseEntity<?> assignAsset(UUID assetId, ClientAssetAssignment assignment) {
        try {
            ClientAsset asset = assetRepository.findById(assetId)
                    .orElseThrow(() -> new RuntimeException("Client asset not found!"));

            long assignedCount =
                    assignmentRepository.countByAsset_AssetIdAndActiveTrue(assetId);

            if (assignedCount >= asset.getQuantity()) {
                throw  new RuntimeException("Assignment failed. No available assets to assign.");
            }

            if (assignment.getSerialNumber() == null ||
                    assignment.getSerialNumber().isBlank()) {
                throw new RuntimeException("Serial number is mandatory while assigning asset");
            }

            String serial = assignment.getSerialNumber().trim().toUpperCase();


            if (assignmentRepository.existsBySerialNumber(serial)) {
                if (assignmentRepository.existsBySerialNumberAndAssignmentStatus(serial, EnablementAssignmentStatus.ASSIGNED)) {
                    throw new RuntimeException("Serial number already assigned to another asset");
                }
            }

            assignment.setSerialNumber(serial);
            assignment.setAsset(asset);
            assignment.setAssignmentStatus(EnablementAssignmentStatus.ASSIGNED);
//            assignment.setAssignedDate(LocalDate.now());
            assignment.setActive(true);

            LocalDate today = LocalDate.now();

            if (assignment.getAssignedDate() != null &&
                    !assignment.getAssignedDate().isEqual(today)) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(
                        false,
                        "Assigned date must be today's date.",
                        null
                ));
            }

            assignment.setAssignedDate(today);


            assignmentRepository.save(assignment);

            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Asset assigned successfully",
                    null
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    false,
                    "Asset assignment failed: " + e.getMessage(),
                    null
            ));
        }
    }


    // UPDATE ASSIGNMENT
    @Override
    public ApiResponse<Void> updateAssignment(
            UUID assignmentId,
            ClientAssetAssignment updated) {

        try {
            ClientAssetAssignment assignment =
                    assignmentRepository.findById(assignmentId)
                            .orElseThrow(() -> new RuntimeException("Assignment not found"));

            // 🔴 SERIAL NUMBER UPDATE VALIDATION
            if (updated.getSerialNumber() != null &&
                    !updated.getSerialNumber().isBlank()) {

                String serial = updated.getSerialNumber().trim().toUpperCase();

                if (assignmentRepository
                        .existsBySerialNumberAndAssignmentIdNot(serial, assignmentId)) {
                    throw new RuntimeException("Serial number already assigned to another asset");
                }

                assignment.setSerialNumber(serial);
            }

            assignment.setResourceName(updated.getResourceName());
            assignment.setProjectName(updated.getProjectName());
            assignment.setExpectedReturnDate(updated.getExpectedReturnDate());
            assignment.setAssignedBy(updated.getAssignedBy());
            assignment.setLocationType(updated.getLocationType());
            assignment.setLocationDetails(updated.getLocationDetails());
            assignment.setDescription(updated.getDescription());

            if(updated.getAssignmentStatus()!=null){
                assignment.setAssignmentStatus(updated.getAssignmentStatus());
            }

            assignmentRepository.save(assignment);

            return new ApiResponse<>(
                    true,
                    "Asset assignment updated successfully",
                    null
            );

        } catch (Exception e) {
            return new ApiResponse<>(
                    false,
                    "Asset assignment update failed: " + e.getMessage(),
                    null
            );
        }
    }


    // DELETE (SOFT)
    @Override
    public ResponseEntity<?> deleteAssignment(UUID assignmentId) {
        ClientAssetAssignment assignment =
                assignmentRepository.findById(assignmentId)
                        .orElseThrow(() ->
                                new ClientException("Assignment not found"));

        if (assignment.getAssignmentStatus() == EnablementAssignmentStatus.ASSIGNED) {
            return ResponseEntity.badRequest().body("Assign Asset can't be delete. Please return it first!");
        }


        assignment.setActive(false);
        assignment.setAssignmentStatus(EnablementAssignmentStatus.RETURNED);

        assignmentRepository.save(assignment);

        return ResponseEntity.ok( new ApiResponse<>(
                true,
                "Asset assignment deleted successfully",
                null
        ));
    }

    // GET ALL ACTIVE
    @Override
    public ApiResponse<?> getAllAssignments() {
        try {
            return new ApiResponse<>(
                    true,
                    "All asset assignments retrieved successfully",
                    assignmentRepository.findAll()
            );
        } catch (Exception e) {
            return new ApiResponse<>(
                    false,
                    "Failed to fetch assignments: " + e.getMessage(),
                    null
            );
        }
    }

    @Override
    public ApiResponse<?> getAssignmentsByAssetId(UUID assetId) {
        ClientAsset asset =  assetRepository.findById(assetId).orElseThrow(() -> new ClientException("Asset Not Found!"));
        AssetResponseDTO assetResponse = new AssetResponseDTO(
                asset.getAssetId(),
                asset.getAssetName(),
                asset.getAssetCategory(),
                asset.getAssetType(),
                asset.getQuantity(),
                asset.getStatus()
        );
        List<ClientAssetAssignment> assignments = assignmentRepository.findByAsset_AssetId(assetId).orElseThrow(() -> new ClientException("Asset Not Found!"));
        List<AssetAssignmentDTO> assignmentsDTO = assignments.stream().map(a -> new AssetAssignmentDTO(
                a.getAssignmentId(),
                a.getResourceName(),
                a.getProjectName(),
                a.getAssignedDate(),
                a.getActualReturnDate(),
                a.getAssignmentStatus(),
                a.getSerialNumber(),
                a.getLocationDetails(),
                a.getAssignedBy(),
                a.getDescription()
        )).toList();

        return new ApiResponse<>(true,"Fetched Assigned Assets Successfully", Map.of("asset",assetResponse,"assignments",assignmentsDTO));
    }

    // RETURN ASSET (LOCKED API)
    @Override
    public ResponseEntity<ApiResponse<AssetAssignmentKPIDTo>> getKPI(UUID assetId) {
        ClientAsset asset = assetRepository.findById(assetId).orElseThrow(() -> new ClientException("No Asset Found with the ID."));
        long totalAssets = asset.getQuantity();
        long activeAssignments = assignmentRepository.countByAsset_AssetIdAndAssignmentStatus(assetId, EnablementAssignmentStatus.ASSIGNED);
        long availableAssets = Math.max(0, (totalAssets - activeAssignments));
        long utilization = totalAssets > 0 ? Math.round((double) activeAssignments / totalAssets * 100) : 0;
        return ResponseEntity.ok(new ApiResponse<>(true,"Fetched KPI Successfully", new AssetAssignmentKPIDTo(totalAssets, activeAssignments, availableAssets, utilization)));
    }

    @Override
    public ApiResponse<Void> returnAsset(
            UUID assignmentId,
            LocalDate actualReturnDate,
            String remarks) {

        try {
            ClientAssetAssignment assignment =
                    assignmentRepository.findById(assignmentId)
                            .orElseThrow(() ->
                                    new RuntimeException("Assignment not found"));

            if (assignment.getAssignmentStatus() == EnablementAssignmentStatus.RETURNED) {
                throw new RuntimeException("Asset already returned");
            }

            if (actualReturnDate == null) {
                throw new RuntimeException("Actual return date is required");
            }

            assignment.setActualReturnDate(actualReturnDate);
            assignment.setDescription(remarks);
            assignment.setAssignmentStatus(EnablementAssignmentStatus.RETURNED);
            assignment.setActive(false);

            assignmentRepository.save(assignment);

            return new ApiResponse<>(
                    true,
                    "Asset returned successfully",
                    null
            );
        } catch (Exception e) {
            return new ApiResponse<>(
                    false,
                    "Asset return failed: " + e.getMessage(),
                    null
            );
        }
    }
}
