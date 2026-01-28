package com.controller.client_controllers;


import com.dto.ApiResponse;
import com.entity.client_entities.ClientAsset;
import com.service_imple.client_service_impl.ClientAssetServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/clinet-assets")
public class ClientAssetController {
    private final ClientAssetServiceImpl service;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createClientAsset(
            @RequestBody ClientAsset asset) {

        ApiResponse<String> response = service.createClientAsset(asset);

        return ResponseEntity
                .status(response.getSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // FETCH ENABLEMENTS BY CLIENT
    @GetMapping("/client/{clientId}")
    public ResponseEntity<ApiResponse<?>> getAssetsByClient(
            @PathVariable Long clientId) {

        ApiResponse<?> response = service.getAssetsByClient(clientId);

        return ResponseEntity
                .status(response.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }
}
