package com.example.market.catalog;

import jakarta.validation.constraints.Min;

/**
 * Request payload for setting an absolute product stock level.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public record StockPatchRequest(@Min(0) int stock) { }
