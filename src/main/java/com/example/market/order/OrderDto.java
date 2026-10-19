package com.example.market.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Read model representing an order returned by the API.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public record OrderDto(
        Long id, String orderNo, String status, BigDecimal total, String currency,
        String customerEmail, String paymentReference, Instant createdAt, Instant paidAt,
        List<Item> items
) {
    public record Item(String sku, String name, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) { }

    public static OrderDto from(Order o) {
        List<Item> items = o.getItems().stream()
                .map(i -> new Item(i.getSku(), i.getProductName(), i.getUnitPrice(),
                        i.getQuantity(), i.getLineTotal()))
                .toList();
        return new OrderDto(o.getId(), o.getOrderNo(), o.getStatus().name(), o.getTotal(), o.getCurrency(),
                o.getUser().getEmail(), o.getPaymentReference(), o.getCreatedAt(), o.getPaidAt(), items);
    }
}
