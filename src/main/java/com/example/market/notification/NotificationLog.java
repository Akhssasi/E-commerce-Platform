package com.example.market.notification;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA entity recording that an order notification has already been sent.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@Entity
@Table(name = "notification_log")
public class NotificationLog {

    @Id
    @Column(name = "order_no", length = 32, nullable = false)
    private String orderNo;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt = Instant.now();

    protected NotificationLog() { }

    public NotificationLog(String orderNo, String email) {
        this.orderNo = orderNo;
        this.email = email;
    }

    public String getOrderNo() { return orderNo; }
    public String getEmail() { return email; }
    public Instant getSentAt() { return sentAt; }
}
