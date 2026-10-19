package com.example.market.cart;

import com.example.market.catalog.Product;
import jakarta.persistence.*;

/**
 * JPA entity representing a quantity of a product inside a cart.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@Entity
@Table(name = "cart_items",
       uniqueConstraints = @UniqueConstraint(name = "uk_cart_item_product", columnNames = {"cart_id", "product_id"}))
public class CartItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    protected CartItem() { }

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    void attachTo(Cart cart) { this.cart = cart; }
    void detach() { this.cart = null; }

    public void changeQuantity(int quantity) { this.quantity = quantity; }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
}
