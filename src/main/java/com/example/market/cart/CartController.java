package com.example.market.cart;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing shopping cart endpoints for the current user.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Shopping cart for the current user")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Get the current cart")
    public CartDto getCart() {
        return cartService.getCart();
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product to the cart")
    public CartDto addItem(@Valid @RequestBody AddCartItemRequest req) {
        return cartService.addItem(req);
    }

    @PatchMapping("/items/{sku}")
    @Operation(summary = "Update item quantity (0 removes the item)")
    public CartDto updateItem(@PathVariable String sku, @Valid @RequestBody UpdateCartItemRequest req) {
        return cartService.updateItem(sku, req.quantity());
    }

    @DeleteMapping("/items/{sku}")
    @Operation(summary = "Remove an item from the cart")
    public ResponseEntity<CartDto> removeItem(@PathVariable String sku) {
        return ResponseEntity.ok(cartService.removeItem(sku));
    }
}
