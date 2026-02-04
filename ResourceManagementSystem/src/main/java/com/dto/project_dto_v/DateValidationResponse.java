package com.dto.project_dto_v;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DateValidationResponse {
    private boolean valid;
    private String message;
}
