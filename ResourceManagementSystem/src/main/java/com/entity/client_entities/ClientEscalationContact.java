package com.entity.client_entities;

import com.entity_enums.client_enums.ContactRole;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "client_escalation_contact")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClientEscalationContact {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID contactId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private String contactName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactRole contactRole;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(nullable = false)
    private Boolean activeFlag;

    @Column(nullable = false)
    private String escalationLevel;
}
