package com.controller.client_controllers;

import com.dto.ApiResponse;
import com.entity.client_entities.ClientEscalationContact;
import com.service_interface.client_service_interface.ClientContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/client-contact")
public class ClientContactController {
    @Autowired
    ClientContactService clientContactService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> createClientContact(@RequestBody ClientEscalationContact clientContact) {
        return clientContactService.createClientContact(clientContact);
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> updateClientContact(@RequestBody ClientEscalationContact clientContact) {
        return clientContactService.updateClientContact(clientContact);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deleteClientContact(@PathVariable UUID id) {
        return clientContactService.deleteClientContact(id);
    }

    @GetMapping("/clientContact/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<?>> getClientContact(@PathVariable UUID clientId) {
        return clientContactService.getClientContact(clientId);
    }
}
