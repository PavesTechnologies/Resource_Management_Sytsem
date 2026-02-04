package com.dto.project_dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DemandDateValidationRequest {
    private LocalDateTime demandStart;
    private LocalDateTime demandEnd;
}
