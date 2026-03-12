package com.entity.company_entities;

import com.entity_enums.client_enums.ContactRole;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "company_escalation_contact", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"company_id", "email"}, 
                           name = "uk_company_contact_email")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompanyEscalationContact {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "contact_id")
    @JsonProperty("contactId")
    private UUID contactId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, referencedColumnName = "company_id")
    @JsonProperty("company")
    private Company company;

    @NotBlank(message = "Contact name is required")
    @JsonProperty("contactName")
    private String contactName;

    @NotNull(message = "Contact role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonProperty("contactRole")
    private ContactRole contactRole;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(nullable = false)
    @JsonProperty("email")
    private String email;

    @JsonProperty("phone")
    private String phone;

    @NotNull(message = "Active flag is required")
    @Column(nullable = false)
    @JsonProperty("activeFlag")
    private Boolean activeFlag;

    @NotBlank(message = "Escalation level is required")
    @Column(nullable = false)
    @JsonProperty("escalationLevel")
    private String escalationLevel;

    // Custom setter to handle companyId from JSON
    @JsonProperty("companyId")
    public void setCompanyId(UUID companyId) {
        // When companyId is set, create a partial company object
        if (companyId != null) {
            if (this.company == null) {
                this.company = new Company();
            }
            this.company.setCompanyId(companyId);
        }
    }

    // Custom setter to handle clientId from JSON (alternative field name)
    @JsonProperty("clientId")
    public void setClientId(UUID clientId) {
        // Delegate to setCompanyId to avoid code duplication
        setCompanyId(clientId);
    }

    // @ElementCollection
    // @CollectionTable(name = "company_contact_triggers", joinColumns = @JoinColumn(name = "contact_id"))
    // @Column(name = "trigger_type")
    // @JsonProperty("triggers")
    // private List<String> triggers;
}
