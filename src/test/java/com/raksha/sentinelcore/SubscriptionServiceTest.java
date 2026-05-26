package com.raksha.sentinelcore;

import com.raksha.sentinelcore.entity.Subscription;
import com.raksha.sentinelcore.entity.User;
import com.raksha.sentinelcore.enums.PlanType;
import com.raksha.sentinelcore.enums.SubStatus;
import com.raksha.sentinelcore.exception.Exceptions.InvalidPlanTransitionException;
import com.raksha.sentinelcore.exception.Exceptions.SubscriptionNotActiveException;
import com.raksha.sentinelcore.repository.SubscriptionRepository;
import com.raksha.sentinelcore.service.EntitlementService;
import com.raksha.sentinelcore.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock SubscriptionRepository subRepo;
    @Mock EntitlementService     entitlementService;

    @InjectMocks SubscriptionService service;

    private User         user;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@example.com").password("hashed").build();
        subscription = Subscription.builder()
            .id(1L).user(user).plan(PlanType.FREE).status(SubStatus.ACTIVE).build();
    }

    @Test
    void upgrade_validPlan_upgrades() {
        when(subRepo.findByUserId(1L)).thenReturn(Optional.of(subscription));
        when(subRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Subscription result = service.upgrade(1L, PlanType.PRO);

        assertThat(result.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(result.getStatus()).isEqualTo(SubStatus.ACTIVE);
        verify(entitlementService).invalidateCache(1L);
    }

    @Test
    void upgrade_downgrade_throwsException() {
        subscription.setPlan(PlanType.PRO);
        when(subRepo.findByUserId(1L)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> service.upgrade(1L, PlanType.FREE))
            .isInstanceOf(InvalidPlanTransitionException.class);
    }

    @Test
    void pause_activeSubscription_pauses() {
        when(subRepo.findByUserId(1L)).thenReturn(Optional.of(subscription));
        when(subRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Subscription result = service.pause(1L);
        assertThat(result.getStatus()).isEqualTo(SubStatus.PAUSED);
    }

    @Test
    void pause_alreadyPaused_throwsException() {
        subscription.setStatus(SubStatus.PAUSED);
        when(subRepo.findByUserId(1L)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> service.pause(1L))
            .isInstanceOf(SubscriptionNotActiveException.class);
    }

    @Test
    void cancel_anyStatus_cancels() {
        when(subRepo.findByUserId(1L)).thenReturn(Optional.of(subscription));
        when(subRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Subscription result = service.cancel(1L);

        assertThat(result.getStatus()).isEqualTo(SubStatus.CANCELLED);
        verify(entitlementService).invalidateCache(1L);
    }
}
