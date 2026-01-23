package com.controller.client_controllers;

import com.dto.ApiResponse;
import com.entity.client_entities.Client;
import com.service_interface.client_service_interface.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/client")
@CrossOrigin
public class ClientController {

    @Autowired
    ClientService clientService;

    @PostMapping("create")
    @PreAuthorize("hasRole('HR-MANAGER')")
    public ResponseEntity<ApiResponse> createClient(@RequestBody Client client)
    {
        return clientService.createClient(client);
    }
}
