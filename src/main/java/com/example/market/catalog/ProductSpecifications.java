package com.example.market.catalog;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Composable JPA specifications for filtering the product catalog.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public final class ProductSpecifications {

    private ProductSpecifications() { }

    public static Specification<Product> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Product> nameOrSkuContains(String q) {
        if (q == null || q.isBlank()) return null;
        String pattern = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("sku")), pattern));
    }

    public static Specification<Product> inCategory(Long categoryId) {
        if (categoryId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> inCategoryNamed(String category) {
        if (category == null || category.isBlank()) return null;
        String name = category.trim().toLowerCase();
        return (root, query, cb) -> cb.equal(cb.lower(root.get("category").get("name")), name);
    }

    public static Specification<Product> minPrice(BigDecimal min) {
        if (min == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> maxPrice(BigDecimal max) {
        if (max == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }
}
