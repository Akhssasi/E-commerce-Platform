package com.example.market.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Command payload describing the lines requested in a purchase.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public record PlaceOrderCommand(@NotEmpty @Valid List<Line> lines) {

    public record Line(@NotBlank String sku, @Min(1) int quantity) { }
}
