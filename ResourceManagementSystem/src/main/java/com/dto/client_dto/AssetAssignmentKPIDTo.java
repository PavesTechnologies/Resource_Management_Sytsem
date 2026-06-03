package com.dto.client_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetAssignmentKPIDTo {
    private long totalAssets;
    private long activeAssets;
    private long availableAssets;
    private long utilization;
}
