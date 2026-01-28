package com.service_interface.client_service_interface;

import com.dto.ApiResponse;
import com.entity.client_entities.ClientCompliance;
import org.springframework.http.ResponseEntity;

public interface ClientComplianceService {
    ResponseEntity<ApiResponse> createClientCompliance(ClientCompliance clientCompliance);
    ResponseEntity<ApiResponse> updateClientCompliance(ClientCompliance clientCompliance);
    ResponseEntity<ApiResponse> deleteClientCompliance(Long id);

    ResponseEntity<ApiResponse> getClientCompliance(Long clientId);
}
