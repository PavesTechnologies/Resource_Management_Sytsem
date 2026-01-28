package com.service_imple.client_service_impl;


import com.dto.ApiResponse;
import com.entity.client_entities.Client;
import com.entity.client_entities.ClientAsset;
import com.entity_enums.client_enums.AssetStatus;
import com.repo.client_repo.ClientAssetRepository;
import com.repo.client_repo.ClientRepo;
import com.service_interface.client_service_interface.ClientAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClientAssetServiceImpl implements ClientAssetService {

    private final ClientAssetRepository assetRepository;
    private final ClientRepo clientRepository;

    @Override
    public ApiResponse<String> createClientAsset(ClientAsset asset) {
        try {
            if (asset.getClient() == null || asset.getClient().getClientId() == null) {
                throw new RuntimeException("Client ID is required");
            }

            Client client = clientRepository.findById(
                    asset.getClient().getClientId()
            ).orElseThrow(() -> new RuntimeException("Client not found"));

            asset.setClient(client);
            asset.setStatus(AssetStatus.ACTIVE);
            asset.setCreatedAt(LocalDateTime.now());

            assetRepository.save(asset);

            return new ApiResponse<>(
                    true,
                    "Client asset created successfully.",
                    null
            );

        } catch (Exception e) {
            return new ApiResponse<>(
                    false,
                    "Client asset creation failed: " + e.getMessage(),
                    null
            );
        }
    }

    @Override
    public ApiResponse<?> getAssetsByClient(Long clientId) {
        try {
            return new ApiResponse<>(
                    true,
                    "Client assets fetched successfully.",
                    assetRepository.findByClient_ClientId(clientId)
            );
        } catch (Exception e) {
            return new ApiResponse<>(
                    false,
                    "Failed to fetch client assets: " + e.getMessage(),
                    null
            );
        }
    }
}
