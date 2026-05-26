package com.raksha.sentinelcore.dto;

import com.raksha.sentinelcore.enums.PlanType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka message payload from the payment service.
 *
 * <p>On SUCCESS: SentinelCore upgrades the subscription and invalidates Redis cache.
 * On FAILED: event is logged only. Dead-letter queue handling is left to the
 * production infrastructure layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private Long   userId;
    private PlanType plan;
    private String transactionId;
    private String status;  // "SUCCESS" | "FAILED"
}
