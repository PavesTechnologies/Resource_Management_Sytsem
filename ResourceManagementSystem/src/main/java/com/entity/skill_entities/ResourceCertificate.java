package com.entity.skill_entities;

import com.entity_enums.skill_enums.CertificateStatus;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "resource_certificate",
        uniqueConstraints = @UniqueConstraint(columnNames = {"resource_id", "certificate_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "certificate_id", nullable = false)
    private UUID certificateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_id", referencedColumnName = "certificate_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Certificate certificate;

    @Column(name = "issued_date")
    private LocalDate issuedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "active_flag")
    private Boolean activeFlag;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CertificateStatus status;
}
