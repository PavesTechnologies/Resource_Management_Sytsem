package com.entity.project_entities;

import com.entity.client_entities.ClientEscalationContact;
import com.entity_enums.project_enums.EscalationLevel;
import com.entity_enums.project_enums.EscalationTriggerType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "project_escalation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProjectEscalation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private ClientEscalationContact contact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscalationLevel escalationLevel;

    @ElementCollection(targetClass = EscalationTriggerType.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "project_escalation_triggers", joinColumns = @JoinColumn(name = "project_escalation_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type")
    private Set<EscalationTriggerType> triggers;
}
