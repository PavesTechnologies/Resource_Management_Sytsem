package com.entity.skill_entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "certificate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @Column(name = "certificate_id")
    private UUID certificateId;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "certificate_file")
    private String certificateFile;

    @Column(name = "certified_at")
    private LocalDate certifiedAt;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "active_flag")
    private Boolean activeFlag;
}
