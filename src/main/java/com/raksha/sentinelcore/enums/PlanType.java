package com.raksha.sentinelcore.enums;

/**
 * Subscription plan tiers, ordered by ascending privilege.
 * Ordinal is used for upgrade validation (cannot downgrade without cancellation).
 */
public enum PlanType {
    FREE,
    PRO,
    ENTERPRISE
}
