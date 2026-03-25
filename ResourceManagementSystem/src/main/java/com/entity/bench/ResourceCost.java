package com.entity.bench;

import com.entity_enums.bench.Currency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_cost")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long resourceId;

    @Column(nullable = false)
    private BigDecimal costPerDay;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Currency currency = Currency.INR;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
