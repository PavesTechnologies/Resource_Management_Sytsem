package com.entity.skill_entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "delivery_role",
        uniqueConstraints = @UniqueConstraint(columnNames = "role_name")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(name = "role_description")
    private String roleDescription;

    @Column(name = "active_flag")
    private Boolean activeFlag;
}
