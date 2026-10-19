package com.example.market.catalog;

import com.example.market.common.ApiException;
import com.example.market.common.PageResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service that implements catalog search, product management and stock updates.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@Service
public class CatalogService {

    private final ProductRepository products;
    private final CategoryRepository categories;

    public CatalogService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> search(String q, Long categoryId, String category,
                                           BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Specification<Product> spec = Specification.where(ProductSpecifications.activeOnly())
                .and(ProductSpecifications.nameOrSkuContains(q))
                .and(ProductSpecifications.inCategory(categoryId))
                .and(ProductSpecifications.inCategoryNamed(category))
                .and(ProductSpecifications.minPrice(minPrice))
                .and(ProductSpecifications.maxPrice(maxPrice));

        Page<Product> page = products.findAll(spec, pageable);
        return PageResponse.from(page, ProductDto::from);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "products", key = "#sku")
    public ProductDto bySku(String sku) {
        Product p = products.findBySku(sku)
                .orElseThrow(() -> ApiException.notFound("Product", sku));
        return ProductDto.from(p);
    }

    @Transactional
    @CacheEvict(cacheNames = "products", key = "#result.sku()")
    public ProductDto create(CreateProductRequest req) {
        if (products.existsBySku(req.sku())) {
            throw ApiException.conflict("SKU is already in use: " + req.sku());
        }
        Category category = categories.findById(req.categoryId())
                .orElseThrow(() -> ApiException.notFound("Category", req.categoryId()));
        Product p = new Product(category, req.name(), req.sku(), req.price(), req.stock());
        p.setImageUrl(req.imageUrl());
        return ProductDto.from(products.save(p));
    }

    @Transactional
    @CacheEvict(cacheNames = "products", key = "#sku")
    public ProductDto update(String sku, UpdateProductRequest req) {
        Product p = products.findBySku(sku)
                .orElseThrow(() -> ApiException.notFound("Product", sku));
        if (req.name() != null) p.setName(req.name());
        if (req.price() != null) p.changePrice(req.price());
        if (req.imageUrl() != null) p.setImageUrl(req.imageUrl());
        if (req.active() != null) {
            if (req.active()) p.activate(); else p.deactivate();
        }
        if (req.categoryId() != null) {
            Category category = categories.findById(req.categoryId())
                    .orElseThrow(() -> ApiException.notFound("Category", req.categoryId()));
            p.changeCategory(category);
        }
        return ProductDto.from(p);
    }

    @Transactional
    @CacheEvict(cacheNames = "products", key = "#sku")
    public ProductDto changeStock(String sku, int stock) {
        Product p = products.findBySku(sku)
                .orElseThrow(() -> ApiException.notFound("Product", sku));
        if (stock < 0) throw ApiException.badRequest("Stock cannot be negative");
        int diff = stock - p.getStock();
        if (diff >= 0) p.increaseStock(diff); else p.decreaseStock(-diff);
        return ProductDto.from(p);
    }

    @Transactional
    public Category createCategory(String name) {
        if (name == null || name.isBlank()) {
            throw ApiException.badRequest("Category name is required");
        }
        if (categories.findByName(name).isPresent()) {
            throw ApiException.conflict("Category already exists: " + name);
        }
        return categories.save(new Category(name));
    }

    @Transactional(readOnly = true)
    public java.util.List<Category> allCategories() {
        return categories.findAll();
    }
}
