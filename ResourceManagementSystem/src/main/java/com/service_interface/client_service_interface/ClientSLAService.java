package com.service_interface.client_service_interface;

import com.dto.client_dto.ApiResponse;
import com.entity.client_entities.ClientSLA;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface ClientSLAService {

    ResponseEntity<ApiResponse> createClientSLA(ClientSLA clientSLA);
    ResponseEntity<ApiResponse> updateClientSLA(ClientSLA clientSLA);
    ResponseEntity<ApiResponse> deleteClientSLA(UUID id);

    ResponseEntity<ApiResponse> getClientSLA(UUID clientId);
}
