package com.raksha.sentinelcore.dto;

import com.raksha.sentinelcore.entity.Subscription;
import com.raksha.sentinelcore.enums.PlanType;
import com.raksha.sentinelcore.enums.SubStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

public class SubscriptionDtos {

    @Data
    public static class UpgradeRequest {
        @NotNull(message = "Plan is required")
        private PlanType plan;
    }

    @Data
    public static class SubscriptionResponse {
        private Long id;
        private Long userId;
        private String email;
        private PlanType plan;
        private SubStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime updatedAt;

        public static SubscriptionResponse from(Subscription sub) {
            SubscriptionResponse r = new SubscriptionResponse();
            r.id        = sub.getId();
            r.userId    = sub.getUser().getId();
            r.email     = sub.getUser().getEmail();
            r.plan      = sub.getPlan();
            r.status    = sub.getStatus();
            r.startedAt = sub.getStartedAt();
            r.updatedAt = sub.getUpdatedAt();
            return r;
        }
    }
}
