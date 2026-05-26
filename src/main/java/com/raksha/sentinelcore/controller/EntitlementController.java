package com.raksha.sentinelcore.controller;

import com.raksha.sentinelcore.service.EntitlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Entitlement check endpoints — the hot path served from Redis at 5,000+ req/s.
 *
 * <pre>
 * GET /api/entitlements/{userId}                      → full entitlement map
 * GET /api/entitlements/{userId}/check?feature=X      → boolean check for a single feature
 * </pre>
 *
 * <p>Both endpoints go through {@link EntitlementService#getEntitlements}, which
 * returns from Redis on cache HIT (sub-millisecond) and hits PostgreSQL only on MISS.
 */
@RestController
@RequestMapping("/api/entitlements")
@RequiredArgsConstructor
public class EntitlementController {

    private final EntitlementService entitlementService;

    /**
     * Returns the full feature map for a user.
     * <br>Example response: {@code {"api_calls_per_day": true, "export_csv": false, "custom_sso": false}}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Boolean>> getEntitlements(@PathVariable Long userId) {
        return ResponseEntity.ok(entitlementService.getEntitlements(userId));
    }

    /**
     * Checks whether a specific feature is enabled for a user.
     * Returns {@code false} if the feature is unknown or disabled.
     */
    @GetMapping("/{userId}/check")
    public ResponseEntity<Boolean> hasFeature(
            @PathVariable Long userId,
            @RequestParam String feature) {
        Map<String, Boolean> entitlements = entitlementService.getEntitlements(userId);
        return ResponseEntity.ok(entitlements.getOrDefault(feature, false));
    }
}
