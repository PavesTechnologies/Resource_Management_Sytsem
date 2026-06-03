package com.dto.client_dto;

import com.entity_enums.client_enums.AssetCategory;
import com.entity_enums.client_enums.AssetStatus;
import lombok.*;

import java.util.UUID;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AssetResponseDTO {
    private UUID assetId;
    private String assetName;
    private AssetCategory assetCategory;
    private String assetType;
    private Integer quantity;
    private AssetStatus status;
}
