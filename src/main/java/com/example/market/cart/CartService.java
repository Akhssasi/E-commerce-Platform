package com.example.market.cart;

import com.example.market.auth.User;
import com.example.market.catalog.Product;
import com.example.market.catalog.ProductRepository;
import com.example.market.common.ApiException;
import com.example.market.common.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that manages cart items and enforces stock and availability rules.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@Service
public class CartService {

    private final CartRepository carts;
    private final ProductRepository products;

    public CartService(CartRepository carts, ProductRepository products) {
        this.carts = carts;
        this.products = products;
    }

    @Transactional
    public CartDto getCart() {
        return CartDto.from(loadOrCreate(SecurityUtils.currentUser()));
    }

    @Transactional
    public CartDto addItem(AddCartItemRequest req) {
        User user = SecurityUtils.currentUser();
        Cart cart = loadOrCreate(user);

        Product product = products.findBySku(req.sku())
                .orElseThrow(() -> ApiException.notFound("Product", req.sku()));
        if (!product.isActive()) {
            throw ApiException.badRequest("Product is not available for sale: " + product.getSku());
        }

        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getSku().equals(product.getSku()))
                .findFirst().orElse(null);

        int newQty = req.quantity() + (existing == null ? 0 : existing.getQuantity());
        if (newQty > product.getStock()) {
            throw ApiException.conflict("Insufficient stock. Available: " + product.getStock());
        }

        if (existing == null) {
            cart.addItem(new CartItem(product, req.quantity()));
        } else {
            existing.changeQuantity(newQty);
        }
        return CartDto.from(carts.save(cart));
    }

    @Transactional
    public CartDto updateItem(String sku, int quantity) {
        Cart cart = loadOrCreate(SecurityUtils.currentUser());
        CartItem item = findItem(cart, sku);

        if (quantity <= 0) {
            cart.removeItem(item);
            return CartDto.from(carts.save(cart));
        }
        if (quantity > item.getProduct().getStock()) {
            throw ApiException.conflict("Insufficient stock. Available: " + item.getProduct().getStock());
        }
        item.changeQuantity(quantity);
        return CartDto.from(carts.save(cart));
    }

    @Transactional
    public CartDto removeItem(String sku) {
        Cart cart = loadOrCreate(SecurityUtils.currentUser());
        cart.removeItem(findItem(cart, sku));
        return CartDto.from(carts.save(cart));
    }

    @Transactional
    public void clear() {
        Cart cart = loadOrCreate(SecurityUtils.currentUser());
        cart.getItems().forEach(cart::removeItem);
        carts.save(cart);
    }

    private CartItem findItem(Cart cart, String sku) {
        return cart.getItems().stream()
                .filter(i -> i.getProduct().getSku().equals(sku))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Cart item", sku));
    }

    private Cart loadOrCreate(User user) {
        return carts.findByUserId(user.getId()).orElseGet(() -> carts.save(new Cart(user)));
    }
}
