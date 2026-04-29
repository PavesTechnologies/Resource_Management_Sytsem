package com.controller.client_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.dto.client_dto.*;
import com.entity.client_entities.Client;
import com.service_interface.client_service_interface.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/client")
@CrossOrigin
public class ClientController {

    @Autowired
    ClientService clientService;

    @PostMapping("create")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Client>> createClient(@RequestBody Client client)
    {
        return clientService.createClient(client);
    }

    @GetMapping("search")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','ADMIN')")
    public ApiResponse<PageResponse<ClientDTO>> searchClients(
            @ModelAttribute ClientFilterDTO filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return clientService.searchClients(filter, page, size);
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<Void>> countClients() {
        return clientService.countClients();
    }

    @GetMapping("/get-all-clients")
    @PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<List<Client>>> getClientDetails() {
        return clientService.clientDetails();
    }

    @GetMapping("/active-clients")
    @PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER','PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<List<ClientDTO>>> getActiveClients() {
        return clientService.getActiveClients();
    }

    @GetMapping("/get-admin-kpi")
    @PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<AdminKPIDTO>> getAdminKPIDetials() {
        return clientService.getAdminKPI();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER', 'PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<Client>> getClientById(@PathVariable UUID id) {
        return clientService.getClientById(id);
    }

    @GetMapping("/{id}/page-data")
    @PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER', 'PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<ClientProjectStatisticsDTO>> getClientProjectStatistics(@PathVariable UUID id) {
        return clientService.getClientProjectStatistics(id);
    }

    @PutMapping("/update-client")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Client>> updateClientDetails(@RequestBody Client clientDetails) {
        return clientService.updateClient(clientDetails);
    }

    @DeleteMapping("/delete-client/{clientId}")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable UUID clientId) {
        return clientService.deleteClient(clientId);
    }

    @GetMapping("/get-active-clients")
    @PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER','PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<List<ActiveClientsPMSDTO>>> getActiveClientsPMS() {
        return clientService.getActiveClientsPMS();
    }
}
