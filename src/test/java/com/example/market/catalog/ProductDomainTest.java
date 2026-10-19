package com.example.market.catalog;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the stock and pricing rules of the Product entity.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
class ProductDomainTest {

    private Product product(int stock) {
        return new Product(new Category("Test"), "Test Product", "TEST-1", new BigDecimal("10.00"), stock);
    }

    @Test
    void decreaseStock_reducesStock() {
        Product p = product(5);
        p.decreaseStock(3);
        assertThat(p.getStock()).isEqualTo(2);
    }

    @Test
    void decreaseStock_rejectsNonPositiveQuantity() {
        Product p = product(5);
        assertThatThrownBy(() -> p.decreaseStock(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> p.decreaseStock(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decreaseStock_rejectsInsufficientStock() {
        Product p = product(2);
        assertThatThrownBy(() -> p.decreaseStock(3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
        assertThat(p.getStock()).isEqualTo(2);
    }

    @Test
    void changePrice_rejectsInvalidPrice() {
        Product p = product(1);
        assertThatThrownBy(() -> p.changePrice(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> p.changePrice(null))
                .isInstanceOf(IllegalArgumentException.class);

        p.changePrice(new BigDecimal("15.50"));
        assertThat(p.getPrice()).isEqualByComparingTo("15.50");
    }
}
