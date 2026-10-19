package com.example.market.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request payload for partially updating a product.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public record UpdateProductRequest(
        @Size(max = 200) String name,
        @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Size(max = 500) String imageUrl,
        Boolean active,
        Long categoryId
) { }
