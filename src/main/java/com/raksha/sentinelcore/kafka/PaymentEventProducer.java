package com.raksha.sentinelcore.kafka;

import com.raksha.sentinelcore.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes payment events to the {@code payment-events} Kafka topic.
 *
 * <p>In the real architecture this class lives in the <em>payment service</em>,
 * not SentinelCore. It is included here as a dev/test convenience so you can
 * trigger events without a separate service running.
 *
 * @see com.raksha.sentinelcore.controller.TestController
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public void publish(PaymentEvent event) {
        CompletableFuture<SendResult<String, PaymentEvent>> future =
            kafkaTemplate.send(TOPIC, event.getTransactionId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish PaymentEvent — txId={} error={}",
                    event.getTransactionId(), ex.getMessage());
            } else {
                log.info("PaymentEvent published — txId={} partition={} offset={}",
                    event.getTransactionId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        });
    }
}
