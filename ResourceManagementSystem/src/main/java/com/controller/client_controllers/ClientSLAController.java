package com.controller.client_controllers;


import com.dto.centralised_dto.ApiResponse;
import com.entity.client_entities.ClientSLA;
import com.service_interface.client_service_interface.ClientSLAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/client-sla")
public class ClientSLAController {

    @Autowired
    ClientSLAService clientSLAService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<ClientSLA>> createClientSLA(@RequestBody ClientSLA clientSLA) {
        return clientSLAService.createClientSLA(clientSLA);
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<ClientSLA>> updateClientSLA(@RequestBody ClientSLA clientSLA) {
        return clientSLAService.updateClientSLA(clientSLA);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<ClientSLA>> deleteClientSLA(@PathVariable UUID id) {
        return clientSLAService.deleteClientSLA(id);
    }

    @GetMapping("/clientSLA/{clientId}")
    @PreAuthorize("hasAnyRole('Admin','Resource_Manager', 'Project_Manager')")
    public ResponseEntity<ApiResponse<List<ClientSLA>>> getClientSLA(@PathVariable UUID clientId) {
        return clientSLAService.getClientSLA(clientId);
    }
}
