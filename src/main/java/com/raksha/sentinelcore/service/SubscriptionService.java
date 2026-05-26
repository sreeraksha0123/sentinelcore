package com.raksha.sentinelcore.service;

import com.raksha.sentinelcore.entity.Subscription;
import com.raksha.sentinelcore.entity.User;
import com.raksha.sentinelcore.enums.PlanType;
import com.raksha.sentinelcore.enums.SubStatus;
import com.raksha.sentinelcore.exception.Exceptions.InvalidPlanTransitionException;
import com.raksha.sentinelcore.exception.Exceptions.ResourceNotFoundException;
import com.raksha.sentinelcore.exception.Exceptions.SubscriptionNotActiveException;
import com.raksha.sentinelcore.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages subscription lifecycle state machines.
 *
 * <h3>Business rules</h3>
 * <ul>
 *   <li>Upgrade: plan must increase in tier (FREE → PRO → ENTERPRISE). Downgrade requires cancel first.
 *   <li>Pause: subscription must be ACTIVE.
 *   <li>Cancel: always allowed; triggers cache invalidation.
 * </ul>
 *
 * <p>Cache invalidation is always called after any state change that affects entitlements.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subRepo;
    private final EntitlementService     entitlementService;

    // ── Queries ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Subscription getByUser(User user) {
        return subRepo.findByUserId(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "No subscription for userId=" + user.getId()));
    }

    // ── Mutations ────────────────────────────────────────────

    public Subscription upgrade(Long userId, PlanType newPlan) {
        Subscription sub = getOrThrow(userId);

        if (newPlan.ordinal() <= sub.getPlan().ordinal()) {
            throw new InvalidPlanTransitionException(
                "Downgrade not allowed via this endpoint. Current: "
                    + sub.getPlan() + ", requested: " + newPlan
                    + ". Cancel first, then re-subscribe.");
        }

        sub.setPlan(newPlan);
        sub.setStatus(SubStatus.ACTIVE);
        Subscription saved = subRepo.save(sub);

        // Entitlements changed — force cache refresh on next request
        entitlementService.invalidateCache(userId);

        log.info("Subscription upgraded: userId={} plan={}", userId, newPlan);
        return saved;
    }

    public Subscription pause(Long userId) {
        Subscription sub = getOrThrow(userId);

        if (sub.getStatus() != SubStatus.ACTIVE) {
            throw new SubscriptionNotActiveException(
                "Only ACTIVE subscriptions can be paused. Current status: " + sub.getStatus());
        }

        sub.setStatus(SubStatus.PAUSED);
        log.info("Subscription paused: userId={}", userId);
        return subRepo.save(sub);
    }

    public Subscription cancel(Long userId) {
        Subscription sub = getOrThrow(userId);
        sub.setStatus(SubStatus.CANCELLED);

        // Cancellation resets entitlements — invalidate cache
        entitlementService.invalidateCache(userId);

        log.info("Subscription cancelled: userId={}", userId);
        return subRepo.save(sub);
    }

    // ── Private ──────────────────────────────────────────────

    private Subscription getOrThrow(Long userId) {
        return subRepo.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Subscription not found for userId=" + userId));
    }
}
