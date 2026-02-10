package com.dto.project_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckDemandCreationDTO {
    private boolean create;
    private String reason;
}
