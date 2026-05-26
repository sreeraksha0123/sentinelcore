package com.raksha.sentinelcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SentinelCore — Subscription &amp; Entitlement Engine
 *
 * <p>Production-grade backend that manages subscription lifecycles (FREE → PRO → ENTERPRISE),
 * enforces feature entitlements per tier, and serves entitlement checks at 5,000+ req/s
 * using Redis cache populated on login and invalidated on plan change.
 *
 * <p>Payment events arrive via Kafka, triggering DB updates + cache invalidation.
 */
@SpringBootApplication
public class SentinelCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SentinelCoreApplication.class, args);
    }
}
