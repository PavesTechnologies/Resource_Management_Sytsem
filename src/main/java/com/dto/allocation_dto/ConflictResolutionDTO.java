package com.dto.allocation_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConflictResolutionDTO {
    private UUID conflictId;
    private String resolutionAction; // "UPGRADE", "DISPLACE", "KEEP_CURRENT"
    private String reason;
    private String resolvedBy;
    private LocalDateTime resolutionDate;
    private String resolutionNotes;
}
