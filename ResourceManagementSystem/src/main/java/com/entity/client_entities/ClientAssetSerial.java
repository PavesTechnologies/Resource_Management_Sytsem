package com.entity.client_entities;

import com.entity_enums.client_enums.AssetStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ClientAssetSerial entity representing individual serial numbers for client assets.
 * Ensures global uniqueness of serial numbers across the system.
 */
@Entity
@Table(name = "client_asset_serial",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_client_asset_serial_number",
                        columnNames = {"serial_number"}
                )
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClientAssetSerial {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "asset_serial_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_client_asset_serial_asset"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ClientAsset asset;

    @Column(nullable = false, length = 100, unique = true)
    @NotBlank(message = "Serial number is required")
    @Size(min = 1, max = 100, message = "Serial number must be between 1 and 100 characters")
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status = AssetStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean assigned = false;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
