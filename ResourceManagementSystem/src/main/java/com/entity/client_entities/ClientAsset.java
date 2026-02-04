package com.entity.client_entities;


import com.entity_enums.client_enums.AssetCategory;
import com.entity_enums.client_enums.AssetStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "client_asset")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class ClientAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID assetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Client client;

    @Column(nullable = false)
    private String assetName;

//    @Column
//    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetCategory assetCategory;

    @Column(nullable = false)
    private String assetType;

//    @Column(name = "serial_number", unique = true)
//    private String serialNumber;

    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
