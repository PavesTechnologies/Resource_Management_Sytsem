package com.service_interface.client_service_interface;

import com.dto.*;
import com.entity.client_entities.Client;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface ClientService {
    ResponseEntity<ApiResponse> createClient(Client client);
    ApiResponse<PageResponse<ClientDTO>> searchClients(
            ClientFilterDTO filter,
            int page,
            int size
    );

    ResponseEntity<ApiResponse> countClients();
    ResponseEntity<ApiResponse<List<Client>>> clientDetails();
    ResponseEntity<ApiResponse<Client>> getClientById(UUID id);

    ResponseEntity<ApiResponse<AdminKPIDTO>> getAdminKPI();
    ResponseEntity<ApiResponse<ClientProjectStatisticsDTO>> getClientProjectStatistics(UUID clientId);

    ResponseEntity<ApiResponse<Client>> updateClient(Client client);
    ResponseEntity<ApiResponse> deleteClient(UUID id);

}
