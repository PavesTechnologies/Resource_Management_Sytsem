package com.controller.client_controllers;

import com.dto.ApiResponse;
import com.entity.client_entities.ClientCompliance;
import com.service_interface.client_service_interface.ClientComplianceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/client-compliance")
public class ClientComplianceController {
    @Autowired
    ClientComplianceService clientComplianceService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> createClientCompliance(@RequestBody ClientCompliance clientCompliance) {
        return clientComplianceService.createClientCompliance(clientCompliance);
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateClientCompliance(@RequestBody ClientCompliance clientCompliance) {
        return clientComplianceService.updateClientCompliance(clientCompliance);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteClientCompliance(@PathVariable Long id) {
        return clientComplianceService.deleteClientCompliance(id);
    }

    @GetMapping("/clientCompliance/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse> getClientCompliance(@PathVariable Long clientId) {
        return clientComplianceService.getClientCompliance(clientId);
    }
}
