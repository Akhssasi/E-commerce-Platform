package com.example.market.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent Kafka consumer that sends one order-confirmation email per order number.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@Component
@ConditionalOnProperty(name = "market.outbox.consumer-enabled", havingValue = "true")
public class OrderNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationConsumer.class);

    private final NotificationLogRepository notifications;
    private final ObjectMapper objectMapper;

    public OrderNotificationConsumer(NotificationLogRepository notifications, ObjectMapper objectMapper) {
        this.notifications = notifications;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${market.outbox.topic}", groupId = "${spring.kafka.consumer.group-id:notifications}")
    @Transactional
    public void onOrderPlaced(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String orderNo = node.path("orderNo").asText();
            String email = node.path("customerEmail").asText();

            if (orderNo.isBlank() || notifications.existsById(orderNo)) {
                return;
            }
            notifications.save(new NotificationLog(orderNo, email));
            log.info("[EMAIL] To {}: your order {} has been received. Thank you for shopping with Souqi!",
                    email, orderNo);
        } catch (Exception e) {
            log.error("Failed to process order notification: {}", payload, e);
        }
    }
}
