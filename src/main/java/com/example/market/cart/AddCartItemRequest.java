package com.example.market.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for adding an item to the cart.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public record AddCartItemRequest(
        @NotBlank String sku,
        @Min(1) int quantity
) { }
