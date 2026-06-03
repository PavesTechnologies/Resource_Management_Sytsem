package com.entity.resource_entities;

import com.audit.AuditEntityListener;
import com.entity_enums.resource_enums.EmploymentStatus;
import com.entity_enums.resource_enums.EmploymentType;
import com.entity_enums.resource_enums.WorkingMode;
import jakarta.persistence.Version;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource")
@EntityListeners(AuditEntityListener.class)
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "fieldHandler"})
public class Resource {

    @Id
    @Column(name = "resource_id", length = 20, nullable = false)
    private String resourceId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "email", unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20)
    private EmploymentType employmentType;

    @Column(name = "designation", length = 200)
    private String designation;

    @Column(name = "working_location", length = 100)
    private String workingLocation;

    @Column(name = "experiance", precision = 4)
    private Double experiance;

    @Enumerated(EnumType.STRING)
    @Column(name = "working_mode", length = 20)
    private WorkingMode workingMode;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "date_of_exit")
    private LocalDate dateOfExit;

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 20)
    private EmploymentStatus employmentStatus;

    @Column(name = "notice_start_date")
    private LocalDate noticeStartDate;

    @Column(name = "notice_end_date")
    private LocalDate noticeEndDate;

    @Column(name = "annual_ctc", precision = 15, scale = 2)
    private BigDecimal annualCtc;

    @Column(name = "currency_type", length = 3)
    private String currencyType;

    @Column(name = "hourly_cost_rate", precision = 10, scale = 2)
    private BigDecimal hourlyCostRate;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}