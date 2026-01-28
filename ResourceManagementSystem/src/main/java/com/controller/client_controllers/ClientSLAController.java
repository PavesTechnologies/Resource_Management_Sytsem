package com.controller.client_controllers;


import com.dto.ApiResponse;
import com.entity.client_entities.ClientSLA;
import com.service_interface.client_service_interface.ClientSLAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/client-sla")
public class ClientSLAController {

    @Autowired
    ClientSLAService clientSLAService;

    @PostMapping("create")
    public ResponseEntity<ApiResponse> createClientSLA(@RequestBody ClientSLA clientSLA) {
        return clientSLAService.createClientSLA(clientSLA);
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse> updateClientSLA(@RequestBody ClientSLA clientSLA) {
        return clientSLAService.updateClientSLA(clientSLA);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> deleteClientSLA(@PathVariable Long id) {
        return clientSLAService.deleteClientSLA(id);
    }

    @GetMapping("/clientSLA/{clientId}")
    public ResponseEntity<ApiResponse> getClientSLA(@PathVariable Long clientId) {
        return clientSLAService.getClientSLA(clientId);
    }
}
