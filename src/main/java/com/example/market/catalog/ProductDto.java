package com.example.market.catalog;

import java.math.BigDecimal;

/**
 * Read model representing a product returned by the API.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public record ProductDto(
        Long id, String name, String sku, BigDecimal price, int stock,
        boolean active, String imageUrl, String category, Long categoryId
) {
    public static ProductDto from(Product p) {
        return new ProductDto(p.getId(), p.getName(), p.getSku(), p.getPrice(), p.getStock(),
                p.isActive(), p.getImageUrl(), p.getCategory().getName(), p.getCategory().getId());
    }
}
