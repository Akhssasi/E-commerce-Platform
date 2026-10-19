package com.example.market.catalog;

import com.example.market.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing public catalog browsing and admin product management endpoints.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Catalog", description = "Browse and search products, plus admin management")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/products")
    @Operation(summary = "Search products with pagination and sorting")
    public PageResponse<ProductDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), parseSort(sort));
        return catalog.search(q, categoryId, category, minPrice, maxPrice, pageable);
    }

    @GetMapping("/products/{sku}")
    @Operation(summary = "Get product details by SKU")
    public ProductDto bySku(@PathVariable String sku) {
        return catalog.bySku(sku);
    }

    @PostMapping("/products")
    @Operation(summary = "Create a product [ADMIN]")
    public ResponseEntity<ProductDto> create(@Valid @RequestBody CreateProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalog.create(req));
    }

    @PatchMapping("/products/{sku}")
    @Operation(summary = "Update a product [ADMIN]")
    public ProductDto update(@PathVariable String sku, @Valid @RequestBody UpdateProductRequest req) {
        return catalog.update(sku, req);
    }

    @PatchMapping("/products/{sku}/stock")
    @Operation(summary = "Update product stock [ADMIN]")
    public ProductDto changeStock(@PathVariable String sku, @Valid @RequestBody StockPatchRequest req) {
        return catalog.changeStock(sku, req.stock());
    }

    @GetMapping("/categories")
    @Operation(summary = "List categories")
    public List<Map<String, Object>> categories() {
        return catalog.allCategories().stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName()))
                .toList();
    }

    @PostMapping("/admin/categories")
    @Operation(summary = "Create a category [ADMIN]")
    public ResponseEntity<Map<String, Object>> createCategory(@RequestBody Map<String, String> body) {
        Category c = catalog.createCategory(body.getOrDefault("name", "").trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", c.getId(), "name", c.getName()));
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        if (parts.length == 2) {
            return Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
