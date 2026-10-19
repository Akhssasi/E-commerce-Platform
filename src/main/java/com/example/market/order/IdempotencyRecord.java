package com.example.market.order;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA entity mapping an idempotency key to the order it produced.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", length = 120, nullable = false)
    private String key;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected IdempotencyRecord() { }

    public IdempotencyRecord(String key, Long orderId) {
        this.key = key;
        this.orderId = orderId;
    }

    public String getKey() { return key; }
    public Long getOrderId() { return orderId; }
    public Instant getCreatedAt() { return createdAt; }
}
