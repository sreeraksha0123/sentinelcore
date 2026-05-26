package com.raksha.sentinelcore;

import com.raksha.sentinelcore.entity.Entitlement;
import com.raksha.sentinelcore.entity.Subscription;
import com.raksha.sentinelcore.entity.User;
import com.raksha.sentinelcore.enums.PlanType;
import com.raksha.sentinelcore.enums.SubStatus;
import com.raksha.sentinelcore.repository.EntitlementRepository;
import com.raksha.sentinelcore.repository.SubscriptionRepository;
import com.raksha.sentinelcore.service.EntitlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntitlementServiceTest {

    @Mock RedisTemplate<String, Object>  redis;
    @Mock ValueOperations<String, Object> valueOps;
    @Mock SubscriptionRepository         subRepo;
    @Mock EntitlementRepository          entRepo;

    @InjectMocks EntitlementService service;

    private User         user;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@example.com").password("hashed").build();
        subscription = Subscription.builder()
            .id(1L).user(user).plan(PlanType.PRO).status(SubStatus.ACTIVE).build();

        when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void getEntitlements_cacheHit_returnsFromRedis() {
        Map<String, Boolean> cached = Map.of("export_csv", true, "api_calls_per_day", true);
        when(valueOps.get("entitlement:1")).thenReturn(cached);

        Map<String, Boolean> result = service.getEntitlements(1L);

        assertThat(result).containsEntry("export_csv", true);
        verify(subRepo, never()).findByUserId(any());   // DB not touched
    }

    @Test
    void getEntitlements_cacheMiss_queriesDbAndCaches() {
        when(valueOps.get("entitlement:1")).thenReturn(null);
        when(subRepo.findByUserId(1L)).thenReturn(Optional.of(subscription));
        when(entRepo.findByPlan(PlanType.PRO)).thenReturn(List.of(
            new Entitlement(1L, PlanType.PRO, "export_csv",        true),
            new Entitlement(2L, PlanType.PRO, "api_calls_per_day", true)
        ));

        Map<String, Boolean> result = service.getEntitlements(1L);

        assertThat(result).containsKey("export_csv");
        verify(valueOps).set(eq("entitlement:1"), any(), any());  // cached
    }

    @Test
    void invalidateCache_deletesRedisKey() {
        service.invalidateCache(1L);
        verify(redis).delete("entitlement:1");
    }

    @Test
    void cacheEntitlements_mapsFeaturesToBooleans() {
        when(subRepo.findByUserId(1L)).thenReturn(Optional.of(subscription));
        when(entRepo.findByPlan(PlanType.PRO)).thenReturn(List.of(
            new Entitlement(1L, PlanType.PRO, "export_csv", true),
            new Entitlement(2L, PlanType.PRO, "custom_sso", false)
        ));

        Map<String, Boolean> result = service.cacheEntitlements(1L);

        assertThat(result.get("export_csv")).isTrue();
        assertThat(result.get("custom_sso")).isFalse();
    }
}
