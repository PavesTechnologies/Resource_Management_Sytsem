package com.controller.client_controllers;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client-sla")
public class ClientSLAController {

    @Autowired
    ClientSLAService clientSLAService;

    @PostMapping("create")
    public ResponseEntity<ApiResponse> createClientSLA(@RequestBody ClientSLA clientSLA) {
        return clientSLAService.createClientSLA(clientSLA);
    }
}
