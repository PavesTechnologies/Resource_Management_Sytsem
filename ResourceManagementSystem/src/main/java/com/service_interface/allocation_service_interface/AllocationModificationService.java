package com.service_interface.allocation_service_interface;

import com.dto.ApiResponse;
import com.dto.UserDTO;
import com.dto.allocation_dto.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface AllocationModificationService {

    ResponseEntity<ApiResponse<?>> createModification(CreateAllocationModificationDTO dto, UserDTO userDTO);
    
    ResponseEntity<ApiResponse<?>> processModificationDecision(UUID modificationId, AllocationModificationDecisionDTO dto, UserDTO userDTO);
    
    ResponseEntity<ApiResponse<?>> executeModification(UUID modificationId);
    
    ResponseEntity<ApiResponse<?>> cancelModification(UUID modificationId, UserDTO userDTO);
    
    ResponseEntity<ApiResponse<AllocationModificationResponseDTO>> getModificationById(UUID modificationId);
    
    ResponseEntity<ApiResponse<List<AllocationModificationResponseDTO>>> getModificationsByProjectManager(UserDTO userDTO);
    
    ResponseEntity<ApiResponse<List<AllocationModificationResponseDTO>>> getModificationsByResourceManager(UserDTO userDTO);
}
