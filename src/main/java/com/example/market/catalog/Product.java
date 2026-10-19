package com.example.market.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity representing a sellable product with optimistic locking and stock rules.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@Entity
@Table(name = "products",
       uniqueConstraints = @UniqueConstraint(name = "uk_product_sku", columnNames = "sku"),
       indexes = {
           @Index(name = "idx_product_active_price", columnList = "active, price"),
           @Index(name = "idx_product_category", columnList = "category_id")
       })
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200) private String name;
    @Column(nullable = false, length = 32)  private String sku;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(nullable = false) private int stock;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "image_url", length = 500) private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Version
    private Long version;

    protected Product() { }

    public Product(Category category, String name, String sku, BigDecimal price, int stock) {
        this.category = category;
        this.name = name;
        this.sku = sku;
        this.price = price;
        this.stock = stock;
    }

    public void decreaseStock(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive");
        if (stock < qty) throw new IllegalStateException("Insufficient stock for product: " + sku);
        stock -= qty;
    }

    public void increaseStock(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive");
        stock += qty;
    }

    public void changePrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.signum() <= 0) {
            throw new IllegalArgumentException("Invalid price");
        }
        this.price = newPrice;
    }

    public void deactivate() { this.active = false; }
    public void activate() { this.active = true; }

    public void changeCategory(Category category) {
        if (category == null) throw new IllegalArgumentException("Category is required");
        this.category = category;
    }

    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public String getName() { return name; }
    public String getSku() { return sku; }
    public BigDecimal getPrice() { return price; }
    public int getStock() { return stock; }
    public boolean isActive() { return active; }
    public String getImageUrl() { return imageUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }

    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setName(String name) { this.name = name; }
}
