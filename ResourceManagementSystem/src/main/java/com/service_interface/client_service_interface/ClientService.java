package com.service_interface.client_service_interface;

import com.dto.ApiResponse;
import com.entity.client_entities.Client;
import org.springframework.http.ResponseEntity;

public interface ClientService {
    ResponseEntity<ApiResponse> createClient(Client client);
}
