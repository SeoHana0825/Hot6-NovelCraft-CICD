package com.example.hot6novelcraft.domain.subscription.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Subscriptions")
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private PlanType planType;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private SubscriptionStatus subscriptionStatus;

    private String failReason;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private LocalDateTime nextBillingAt;
}
