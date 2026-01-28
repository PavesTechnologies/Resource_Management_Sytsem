package com.service_interface.client_service_interface;

import com.dto.ApiResponse;
import com.entity.client_entities.ClientSLA;
import org.springframework.http.ResponseEntity;

public interface ClientSLAService {

    ResponseEntity<ApiResponse> createClientSLA(ClientSLA clientSLA);
    ResponseEntity<ApiResponse> updateClientSLA(ClientSLA clientSLA);
    ResponseEntity<ApiResponse> deleteClientSLA(Long id);

    ResponseEntity<ApiResponse> getClientSLA(Long clientId);
}
