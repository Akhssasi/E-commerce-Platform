package com.example.market.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request payload for creating a product.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public record CreateProductRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 32) String sku,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Min(0) int stock,
        @Size(max = 500) String imageUrl
) { }
