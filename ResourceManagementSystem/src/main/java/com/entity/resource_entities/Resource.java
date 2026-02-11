package com.entity.resource_entities;

import com.entity_enums.resource_enums.EmploymentStatus;
import com.entity_enums.resource_enums.EmploymentType;
import com.entity_enums.resource_enums.WorkforceCategory;
import com.entity_enums.resource_enums.WorkingMode;
import com.security.CurrentUser;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource")
@Data
public class Resource {

    @Id
    private Long resourceId;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "email", unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "workforce_category", nullable = false, length = 20)
    private WorkforceCategory workforceCategory;

    @Column(name = "vendor_name", length = 200)
    private String vendorName;

    @Column(name = "grade", length = 50)
    private String grade;

    @Column(name = "designation", length = 200)
    private String designation;

    @Column(name = "primary_skill_group", length = 100)
    private String primarySkillGroup;

    @Column(name = "working_location", length = 100)
    private String workingLocation;

    @Column(name = "experiance")
    private Long experiance;

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

    @Column(name = "allocation_allowed", nullable = false)
    private Boolean allocationAllowed;

    @Column(name = "annual_ctc", precision = 15, scale = 2)
    private BigDecimal annualCtc;

    @Column(name = "currency_type", length = 3)
    private String currencyType;

    @Column(name = "standard_annual_hours")
    private Integer standardAnnualHours;

    @Column(name = "hourly_cost_rate", precision = 10, scale = 2)
    private BigDecimal hourlyCostRate;

    @Column(name = "status_effective_from")
    private LocalDate statusEffectiveFrom;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @Column(name = "hr_last_synced_at")
    private LocalDateTime hrLastSyncedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
