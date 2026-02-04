package com.dto;

import com.entity_enums.client_enums.AssetCategory;
import com.entity_enums.client_enums.AssetStatus;
import com.entity_enums.client_enums.EnablementAssignmentStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssetAssignmentDTO {
    private UUID assignmentId;
    private String resourceName;
    private String projectName;
    private LocalDate assignedDate;
    private LocalDate actualReturnDate;
    private EnablementAssignmentStatus assignmentStatus;
    private String serialNumber;
    private String locationDetails;
    private String assignedBy;
    private String description;
}
