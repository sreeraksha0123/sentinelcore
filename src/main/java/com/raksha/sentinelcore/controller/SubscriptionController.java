package com.raksha.sentinelcore.controller;

import com.raksha.sentinelcore.dto.SubscriptionDtos.SubscriptionResponse;
import com.raksha.sentinelcore.dto.SubscriptionDtos.UpgradeRequest;
import com.raksha.sentinelcore.entity.User;
import com.raksha.sentinelcore.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Subscription lifecycle endpoints — all require Bearer token.
 *
 * <pre>
 * GET  /api/subscriptions/me       → current subscription details
 * POST /api/subscriptions/upgrade  → upgrade plan {plan: "PRO"|"ENTERPRISE"}
 * POST /api/subscriptions/pause    → pause active subscription
 * POST /api/subscriptions/cancel   → cancel subscription
 * </pre>
 */
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/me")
    public ResponseEntity<SubscriptionResponse> getMySubscription(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
            SubscriptionResponse.from(subscriptionService.getByUser(user)));
    }

    @PostMapping("/upgrade")
    public ResponseEntity<SubscriptionResponse> upgrade(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpgradeRequest req) {
        return ResponseEntity.ok(
            SubscriptionResponse.from(
                subscriptionService.upgrade(user.getId(), req.getPlan())));
    }

    @PostMapping("/pause")
    public ResponseEntity<SubscriptionResponse> pause(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
            SubscriptionResponse.from(subscriptionService.pause(user.getId())));
    }

    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionResponse> cancel(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
            SubscriptionResponse.from(subscriptionService.cancel(user.getId())));
    }
}
