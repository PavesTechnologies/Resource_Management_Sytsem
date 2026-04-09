package com.entity.skill_entities;

import com.entity_enums.skill_enums.CertificateType;
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
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID certificateId;

    @Column(name = "skill_id", nullable = true)
    private UUID skillId;

//    @Enumerated(EnumType.STRING)
//    @Column(name = "certificate_type", nullable = false)
//    private CertificateType certificateType;
// SKILL_BASED | ACHIEVEMENT


    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "certificate_name")
    private String certificateName;

    //    @Column(name = "certified_at")
//    private LocalDate certifiedAt;
//
//    @Column(name = "expiry_date")
//    private LocalDate expiryDate;
    @Column(name = "time_bound")
    private Boolean timeBound;

    @Column(name = "validity_months")
    private Integer validityMonths;

    @Column(name = "active_flag")
    private Boolean activeFlag;
}
