package com.service_imple.client_service_impl;

import com.dto.ApiResponse;
import com.entity.client_entities.Client;
import com.repo.client_repo.ClientRepo;
import com.service_interface.client_service_interface.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Service
public class ClientServiceImple implements ClientService {

    @Autowired
    ClientRepo clientRepo;

    @Override
    public ResponseEntity<ApiResponse> createClient(Client client) {
        Client c = clientRepo.save(client);

        ApiResponse<Client> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(c!=null);
        apiResponse.setMessage(c != null
                ? "Client Created Successfully"
                : "Client Creation Failed");
        apiResponse.setData(c);

        return ResponseEntity.ok(apiResponse);

    }
}
