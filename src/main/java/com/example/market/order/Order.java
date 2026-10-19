package com.example.market.order;

import com.example.market.auth.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a customer order and its line items.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@Entity
@Table(name = "orders",
       uniqueConstraints = @UniqueConstraint(name = "uk_order_no", columnNames = "order_no"),
       indexes = {
           @Index(name = "idx_order_user", columnList = "user_id"),
           @Index(name = "idx_order_status", columnList = "status")
       })
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 32)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "payment_reference", length = 120)
    private String paymentReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "paid_at")
    private Instant paidAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() { }

    public Order(String orderNo, User user) {
        this.orderNo = orderNo;
        this.user = user;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.attachTo(this);
        total = total.add(item.getLineTotal());
    }

    public void markPaid(String paymentReference) {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Cannot pay an order in status: " + status);
        }
        this.status = OrderStatus.PAID;
        this.paymentReference = paymentReference;
        this.paidAt = Instant.now();
    }

    public void markPaymentFailed() {
        this.status = OrderStatus.PAYMENT_FAILED;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public User getUser() { return user; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public String getCurrency() { return currency; }
    public String getPaymentReference() { return paymentReference; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPaidAt() { return paidAt; }
    public List<OrderItem> getItems() { return items; }
}
