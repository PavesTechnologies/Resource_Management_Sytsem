package com.service_interface.client_service_interface;

import com.dto.ApiResponse;
import com.dto.ClientDTO;
import com.dto.ClientFilterDTO;
import com.dto.PageResponse;
import com.entity.client_entities.Client;
import org.springframework.http.ResponseEntity;

public interface ClientService {
    ResponseEntity<ApiResponse> createClient(Client client);
    ApiResponse<PageResponse<ClientDTO>> searchClients(
            ClientFilterDTO filter,
            int page,
            int size
    );

    ResponseEntity<ApiResponse> countClients();
}
