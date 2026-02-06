package com.dto.project_dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DemandRequestDTO {
    private Long projectId;
    private String profileName;
    private int requiredCount;
    private LocalDateTime demandStart;
    private LocalDateTime demandEnd;
}
