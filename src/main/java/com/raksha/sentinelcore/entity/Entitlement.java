package com.raksha.sentinelcore.entity;

import com.raksha.sentinelcore.enums.PlanType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Static mapping of features to plan tiers. Seeded via {@code data.sql}.
 * This table is the source-of-truth that backs the Redis entitlement cache.
 */
@Entity
@Table(
    name = "entitlements",
    uniqueConstraints = @UniqueConstraint(columnNames = {"plan", "feature"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType plan;

    @Column(nullable = false, length = 100)
    private String feature;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
}
