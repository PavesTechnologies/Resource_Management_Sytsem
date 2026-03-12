package com.entity.project_entities;

import com.entity.client_entities.ClientEscalationContact;
import com.entity_enums.client_enums.ContactRole;
import com.entity_enums.project_enums.EscalationLevel;
import com.entity_enums.project_enums.EscalationSource;
import com.entity_enums.project_enums.EscalationTriggerType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "project_escalation", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"project_id", "email"}, 
                           name = "uk_project_escalation_email")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProjectEscalation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "project_escalation_id")
    private UUID projectEscalationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, referencedColumnName = "pms_project_id")
    @JsonIgnore
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", referencedColumnName = "contact_id")
    @JsonIgnore
    private ClientEscalationContact contact;

    @NotBlank(message = "Escalation level is required")
    @Column(nullable = false)
    private String escalationLevel;

    @NotBlank(message = "Contact name is required")
    @Column(nullable = false)
    private String contactName;

    @NotNull(message = "Contact role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactRole contactRole;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(nullable = false)
    private String email;

    private String phone;

    @NotNull(message = "Active flag is required")
    @Column(nullable = false)
    private Boolean activeFlag;

//    @ElementCollection(targetClass = EscalationTriggerType.class, fetch = FetchType.EAGER)
//    @CollectionTable(name = "project_escalation_triggers", joinColumns = @JoinColumn(name = "project_escalation_id"))
//    @Enumerated(EnumType.STRING)
//    @Column(name = "trigger_type")
//    private Set<EscalationTriggerType> triggers;

    @NotNull(message = "Escalation source is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscalationSource source;
}
