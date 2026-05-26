package com.raksha.sentinelcore.controller;

import com.raksha.sentinelcore.dto.PaymentEvent;
import com.raksha.sentinelcore.kafka.PaymentEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Dev-only convenience endpoint for triggering Kafka payment events
 * without running a separate payment service.
 *
 * <p><b>Remove or restrict this controller before deploying to production.</b>
 *
 * <pre>
 * POST /api/test/payment-event
 * Body: { "userId": 1, "plan": "PRO", "transactionId": "txn-abc", "status": "SUCCESS" }
 * </pre>
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final PaymentEventProducer producer;

    @PostMapping("/payment-event")
    public ResponseEntity<Map<String, String>> firePaymentEvent(
            @RequestBody PaymentEvent event) {

        if (event.getTransactionId() == null) {
            event = new PaymentEvent(
                event.getUserId(),
                event.getPlan(),
                "txn-" + UUID.randomUUID(),
                event.getStatus() != null ? event.getStatus() : "SUCCESS"
            );
        }

        log.info("[TEST] Publishing payment event: {}", event);
        producer.publish(event);

        return ResponseEntity.ok(Map.of(
            "message", "Payment event published",
            "transactionId", event.getTransactionId()
        ));
    }
}
