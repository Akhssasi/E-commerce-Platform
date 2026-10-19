package com.example.market.order;

import com.example.market.auth.User;
import com.example.market.common.ApiException;
import com.example.market.payment.PaymentGatewayClient;
import com.example.market.payment.PaymentGatewayClient.PaymentUnavailableException;
import org.springframework.stereotype.Service;

/**
 * Orchestrates checkout: reserve stock in a transaction, charge outside it, then persist the final status.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@Service
public class OrderFacade {

    private final OrderService orderService;
    private final PaymentGatewayClient paymentGateway;

    public OrderFacade(OrderService orderService, PaymentGatewayClient paymentGateway) {
        this.orderService = orderService;
        this.paymentGateway = paymentGateway;
    }

    public OrderDto purchase(PlaceOrderCommand cmd, User user, String idempotencyKey) {
        String key = normalizeKey(idempotencyKey, user);
        OrderDto placed = orderService.place(cmd, user, key);
        return settle(placed, user, key);
    }

    public OrderDto purchaseFromCart(User user, String idempotencyKey) {
        String key = normalizeKey(idempotencyKey, user);
        OrderDto placed = orderService.placeFromCart(user, key);
        return settle(placed, user, key);
    }

    private OrderDto settle(OrderDto placed, User user, String key) {
        if (!OrderStatus.PENDING_PAYMENT.name().equals(placed.status())) {
            return placed; // Replay with the same key: never charge twice.
        }
        try {
            var result = paymentGateway.charge(key, placed.total(), user.getEmail());
            if (!"APPROVED".equalsIgnoreCase(result.status())) {
                orderService.failAndRestoreStock(placed.orderNo());
                throw ApiException.conflict("The payment provider declined the transaction");
            }
            return orderService.markPaid(placed.orderNo(), result.reference());
        } catch (PaymentUnavailableException e) {
            orderService.failAndRestoreStock(placed.orderNo());
            throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "PAYMENT_UNAVAILABLE", "The payment provider is temporarily unavailable, please retry later");
        }
    }

    private String normalizeKey(String header, User user) {
        return (header == null || header.isBlank())
                ? "auto-" + user.getId() + "-" + java.util.UUID.randomUUID()
                : header.trim();
    }
}
