package com.raksha.sentinelcore.service;

import com.raksha.sentinelcore.entity.Entitlement;
import com.raksha.sentinelcore.entity.Subscription;
import com.raksha.sentinelcore.exception.Exceptions.ResourceNotFoundException;
import com.raksha.sentinelcore.repository.EntitlementRepository;
import com.raksha.sentinelcore.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core entitlement service — the hot path running at 5,000+ req/s.
 *
 * <h3>Cache strategy</h3>
 * <ol>
 *   <li><b>Populate on login</b> — {@link UserService#login} calls {@link #cacheEntitlements}
 *       so the first feature check is always a HIT.
 *   <li><b>TTL fallback (1 hour)</b> — handles edge cases where the key expired.
 *   <li><b>Explicit invalidation</b> — called from {@link SubscriptionService} on upgrade/cancel,
 *       ensuring consistency without waiting for TTL expiry.
 * </ol>
 *
 * <h3>Why not @Cacheable?</h3>
 * Manual Redis access gives precise control over TTL and
 * lets us call {@link #cacheEntitlements} eagerly at login time.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EntitlementService {

    private final RedisTemplate<String, Object> redis;
    private final SubscriptionRepository        subRepo;
    private final EntitlementRepository         entRepo;

    private static final String   KEY_PREFIX = "entitlement:";
    private static final Duration TTL        = Duration.ofHours(1);

    // ── Public API ───────────────────────────────────────────

    /**
     * Returns the entitlement map for the given user.
     * Checks Redis first; falls back to DB + re-caches on MISS.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Boolean> getEntitlements(Long userId) {
        String key    = KEY_PREFIX + userId;
        Object cached = redis.opsForValue().get(key);

        if (cached != null) {
            log.debug("Cache HIT  — entitlement:{}", userId);
            return (Map<String, Boolean>) cached;
        }

        log.debug("Cache MISS — entitlement:{} — querying DB", userId);
        return cacheEntitlements(userId);
    }

    /**
     * Fetches entitlements from DB and writes them to Redis.
     * Called eagerly at login and on cache MISS.
     */
    public Map<String, Boolean> cacheEntitlements(Long userId) {
        Subscription sub = subRepo.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Subscription not found for userId=" + userId));

        List<Entitlement> ents = entRepo.findByPlan(sub.getPlan());
        Map<String, Boolean> map = ents.stream()
            .collect(Collectors.toMap(Entitlement::getFeature, Entitlement::isEnabled));

        redis.opsForValue().set(KEY_PREFIX + userId, map, TTL);
        log.debug("Cache SET  — entitlement:{} plan={} features={}", userId, sub.getPlan(), map.size());
        return map;
    }

    /**
     * Deletes the Redis key. The next call to {@link #getEntitlements} will
     * re-query the DB and write a fresh entry.
     */
    public void invalidateCache(Long userId) {
        redis.delete(KEY_PREFIX + userId);
        log.info("Cache INVALIDATED — entitlement:{}", userId);
    }
}
