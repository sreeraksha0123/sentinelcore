package com.raksha.sentinelcore.kafka;

import com.raksha.sentinelcore.dto.PaymentEvent;
import com.raksha.sentinelcore.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code payment-events} from Kafka and drives subscription upgrades.
 *
 * <h3>Why Kafka?</h3>
 * <ul>
 *   <li>Decouples the payment service from SentinelCore — no direct dependency.
 *   <li>If SentinelCore is down, events queue up and replay on restart (no data loss).
 *   <li>Naturally handles backpressure and retry at the Kafka layer.
 * </ul>
 *
 * <h3>Error handling</h3>
 * Failed events are logged. In production, configure a Dead Letter Queue (DLQ)
 * via {@code spring.kafka.listener.defaultDltPartitions} and a {@code @RetryableTopic}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final SubscriptionService subscriptionService;

    @KafkaListener(topics = "payment-events", groupId = "sentinel-group")
    public void handle(PaymentEvent event,
                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                       @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Payment event received — txId={} userId={} plan={} status={} partition={} offset={}",
            event.getTransactionId(), event.getUserId(),
            event.getPlan(), event.getStatus(), partition, offset);

        if (!"SUCCESS".equals(event.getStatus())) {
            log.warn("Ignoring non-SUCCESS payment event — txId={} status={}",
                event.getTransactionId(), event.getStatus());
            return;
        }

        try {
            subscriptionService.upgrade(event.getUserId(), event.getPlan());
            log.info("Subscription upgraded via Kafka — userId={} newPlan={}",
                event.getUserId(), event.getPlan());
        } catch (Exception ex) {
            // In production: send to DLQ and alert
            log.error("Failed to process payment event — txId={} error={}",
                event.getTransactionId(), ex.getMessage(), ex);
        }
    }
}
