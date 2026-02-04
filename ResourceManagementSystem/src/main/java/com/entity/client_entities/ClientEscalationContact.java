package com.entity.client_entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "client_escalation_contact")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientEscalationContact {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID contactId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    private String contactName;
    private String contactRole;
    private String email;
    private String phone;
    private String escalationLevel;
    private Boolean activeFlag;
}

