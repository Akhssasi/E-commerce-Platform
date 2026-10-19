package com.example.market.cart;

import jakarta.validation.constraints.Min;

/**
 * Request payload for changing the quantity of a cart item.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public record UpdateCartItemRequest(@Min(0) int quantity) { }
