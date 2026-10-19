package com.example.market.cart;

import java.math.BigDecimal;
import java.util.List;

/**
 * Read model representing a cart with live prices and computed totals.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public record CartDto(Long id, List<CartItemDto> items, BigDecimal total, int totalItems) {

    public record CartItemDto(Long id, String sku, String name, BigDecimal unitPrice,
                              int quantity, BigDecimal lineTotal, int availableStock) { }

    public static CartDto from(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream().map(i -> new CartItemDto(
                i.getId(),
                i.getProduct().getSku(),
                i.getProduct().getName(),
                i.getProduct().getPrice(),
                i.getQuantity(),
                i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())),
                i.getProduct().getStock()
        )).toList();

        BigDecimal total = items.stream()
                .map(CartItemDto::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int count = items.stream().mapToInt(CartItemDto::quantity).sum();

        return new CartDto(cart.getId(), items, total, count);
    }
}
