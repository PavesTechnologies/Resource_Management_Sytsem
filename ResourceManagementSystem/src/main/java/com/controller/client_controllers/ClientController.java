package com.controller.client_controllers;

import com.dto.*;
import com.entity.client_entities.Client;
import com.service_interface.client_service_interface.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/client")
@CrossOrigin
public class ClientController {

    @Autowired
    ClientService clientService;

    @PostMapping("create")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse> createClient(@RequestBody Client client)
    {
        System.out.println(client.toString());
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
    public ResponseEntity<ApiResponse> countClients() {
        return clientService.countClients();
    }

    @GetMapping("/get-all-clients")
    @PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER', 'PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<List<Client>>> getClientDetails() {
        return clientService.clientDetails();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RESOURCE-MANAGER', 'PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<Client>> getClientById(@PathVariable Long id) {
        return clientService.getClientById(id);
    }

}
