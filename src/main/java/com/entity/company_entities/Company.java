package com.entity.company_entities;

import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.RecordStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonProperty("company_id")
    @Column(name = "company_id")
    private UUID companyId;

    @JsonProperty("company_name")
    @Column(nullable = false)
    @NotBlank(message = "Company name is required")
    @Size(min = 3, max = 100, message = "Company name must be between 3 and 100 characters")
    @Pattern(
            regexp = "^[A-Za-z]+(?:[ .][A-Za-z]+)*$",
            message = "Company name can contain only letters, spaces, and dots"
    )
    private String companyName;

    @JsonProperty("company_code")
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Company code is required")
    @Size(min = 2, max = 10, message = "Company code must be between 2 and 10 characters")
    private String companyCode;

    @JsonProperty("priority_level")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriorityLevel priorityLevel;

    @JsonProperty("country_name")
    @Column(nullable = false)
    private String countryName;

    @JsonProperty("default_timezone")
    @Column(nullable = false)
    private String defaultTimezone;

    @JsonProperty("status")
    @Enumerated(EnumType.STRING)
    private RecordStatus status;

    @JsonProperty("escalation_contact")
    @Column(nullable = false)
    private Boolean escalationContact;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
