package com.entity.client_entities;

import com.audit.AuditEntityListener;
import com.entity_enums.client_enums.ContactRole;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "client_escalation_contact", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"client_id", "email"}, 
                           name = "uk_client_contact_email")
       })
@EntityListeners(AuditEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClientEscalationContact {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "contact_id")
    private UUID contactId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false, referencedColumnName = "client_id")
    private Client client;

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

    @NotBlank(message = "Escalation level is required")
    @Column(nullable = false)
    private String escalationLevel;
}
