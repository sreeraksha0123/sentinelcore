package com.raksha.sentinelcore.enums;

/**
 * Lifecycle states of a subscription.
 *
 * <pre>
 *   PENDING ──► ACTIVE ──► PAUSED ──► CANCELLED
 *                 ▲                        │
 *                 └──────── upgrade ───────┘ (re-activate via payment event)
 * </pre>
 */
public enum SubStatus {
    PENDING,
    ACTIVE,
    PAUSED,
    CANCELLED
}
