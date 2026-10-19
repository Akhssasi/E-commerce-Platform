package com.example.market.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that writes domain events to the outbox within the caller's transaction.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@Service
public class OutboxService {

    private final OutboxRepository outbox;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository outbox, ObjectMapper objectMapper) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    /** Must run in the caller's transaction so the event commits or rolls back with the business change. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String eventType, String aggregateId, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outbox.save(new OutboxEvent(eventType, aggregateId, json));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event", e);
        }
    }
}
