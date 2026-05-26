package com.raksha.sentinelcore.entity;

import com.raksha.sentinelcore.enums.PlanType;
import com.raksha.sentinelcore.enums.SubStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * One-to-one with {@link User}. A subscription is auto-created on user registration
 * at FREE/ACTIVE status. Plan transitions are enforced in {@code SubscriptionService}.
 */
@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PlanType plan = PlanType.FREE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubStatus status = SubStatus.ACTIVE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime startedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
