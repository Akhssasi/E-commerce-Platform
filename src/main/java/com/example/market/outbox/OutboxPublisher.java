package com.example.market.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled publisher that drains pending outbox events to Kafka.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@Component
@ConditionalOnProperty(name = "market.outbox.publisher-enabled", havingValue = "true")
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final String topic;

    public OutboxPublisher(OutboxRepository outbox, KafkaTemplate<String, String> kafka,
                           @Value("${market.outbox.topic}") String topic) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${market.outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishPending() {
        var events = outbox.findTop50ByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING);
        for (var event : events) {
            try {
                kafka.send(topic, event.getAggregateId(), event.getPayload()).get();
                event.markPublished();
                log.info("Outbox published eventId={} type={} aggregate={}",
                        event.getEventId(), event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                event.registerFailure();
                log.warn("Outbox publish failed eventId={} attempts={}",
                        event.getEventId(), event.getAttempts(), e);
            }
        }
    }
}
