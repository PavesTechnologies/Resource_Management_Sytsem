package com.entity.client_entities;


import com.entity_enums.client_enums.EnablementAssignmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "client_asset_assignment")
public class ClientAssetAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID assignmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    @NotNull(message = "Asset must be selected")
    private ClientAsset asset;

    @Column(name = "serial_number", nullable = false)
    @NotBlank(message = "Serial number is required")
    @Size(max = 100, message = "Serial number must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\-_]+$", message = "Serial number must contain only alphanumeric characters, dash, and underscore")
    private String serialNumber;
    @NotBlank(message = "Resource name is required")
    @Size(max = 100, message = "Resource name must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\s_]+$", message = "Resource name must contain only alphabets, numbers, spaces, and underscore")
    private String resourceName;
    @NotBlank(message = "Project name is required")
    @Size(max = 150, message = "Project name must not exceed 150 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\s\\-_]+$", message = "Project name must contain only alphabets, numbers, spaces, dash, and underscore")
    private String projectName;

    @NotNull(message = "Assigned date is required")
    @PastOrPresent(message = "Assigned date cannot be a future date")
    private LocalDate assignedDate;
    @AssertTrue(message = "Expected return date must be today or a future date")
    private boolean isExpectedReturnDateValid() {
        if (expectedReturnDate == null) {
            return true; // Optional field
        }
        LocalDate today = LocalDate.now();
        return !expectedReturnDate.isBefore(today) && 
               (assignedDate == null || !expectedReturnDate.isBefore(assignedDate));
    }
    
    private LocalDate expectedReturnDate;

    @AssertTrue(message = "Actual return date is required when status is RETURNED and cannot be before assigned date")
    private boolean isActualReturnDateValid() {
        if (assignmentStatus == EnablementAssignmentStatus.RETURNED) {
            return actualReturnDate != null && 
                   (assignedDate == null || !actualReturnDate.isBefore(assignedDate));
        }
        return true;
    }
    
    @Column(name="actual_return_date")
    private LocalDate actualReturnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Assignment status is required")
    private EnablementAssignmentStatus assignmentStatus;

    @NotBlank(message = "Assigned by is required")
    @Size(max = 100, message = "Assigned by must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "Assigned by must contain only alphabets and spaces")
    private String assignedBy;
    @NotBlank(message = "Location type is required")
    @Size(max = 100, message = "Location type must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z\\s_]+$", message = "Location type must contain only alphabets, spaces, and underscores")
    private String locationType;
    @Size(max = 255, message = "Location details must not exceed 255 characters")
    private String locationDetails;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Boolean active;
}
