package com.service_interface.client_service_interface;

import com.dto.centralised_dto.ApiResponse;
import com.entity.client_entities.ClientSLA;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface ClientSLAService {

    ResponseEntity<ApiResponse<ClientSLA>> createClientSLA(ClientSLA clientSLA);
    ResponseEntity<ApiResponse<ClientSLA>> updateClientSLA(ClientSLA clientSLA);
    ResponseEntity<ApiResponse<ClientSLA>> deleteClientSLA(UUID id);

    ResponseEntity<ApiResponse<List<ClientSLA>>> getClientSLA(UUID clientId);
}
