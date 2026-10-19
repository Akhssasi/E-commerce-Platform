package com.example.market.order;

import com.example.market.auth.Role;
import com.example.market.auth.User;
import com.example.market.cart.Cart;
import com.example.market.cart.CartRepository;
import com.example.market.catalog.Product;
import com.example.market.catalog.ProductRepository;
import com.example.market.common.ApiException;
import com.example.market.common.PageResponse;
import com.example.market.outbox.OutboxService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service that places orders atomically, manages idempotency and enforces ownership rules.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@Service
public class OrderService {

    private final ProductRepository products;
    private final OrderRepository orders;
    private final IdempotencyRepository idempotency;
    private final CartRepository carts;
    private final OutboxService outbox;

    public OrderService(ProductRepository products, OrderRepository orders,
                        IdempotencyRepository idempotency, CartRepository carts,
                        OutboxService outbox) {
        this.products = products;
        this.orders = orders;
        this.idempotency = idempotency;
        this.carts = carts;
        this.outbox = outbox;
    }

    /**
     * Places an order in a single transaction:
     * 0) check the idempotency key first, so a retry returns the same result;
     * 1) lock each product row and decrease stock (rollback on any failure);
     * 2) build the order and its items;
     * 3) write an Outbox event inside the same transaction.
     */
    @Transactional
    public OrderDto place(PlaceOrderCommand cmd, User user, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = idempotency.findById(idempotencyKey);
            if (existing.isPresent()) {
                Order order = orders.findById(existing.get().getOrderId())
                        .orElseThrow(() -> ApiException.notFound("Order", existing.get().getOrderId()));
                if (!order.getUser().getId().equals(user.getId())) {
                    throw ApiException.conflict("Idempotency key already in use");
                }
                return OrderDto.from(order);
            }
        }

        List<PlaceOrderCommand.Line> lines = normalize(cmd.lines());

        Order order = new Order(newOrderNo(), user);
        for (var line : lines) {
            Product product = products.findBySkuForUpdate(line.sku())
                    .orElseThrow(() -> ApiException.notFound("Product", line.sku()));
            if (!product.isActive()) {
                throw ApiException.badRequest("Product is not available for sale: " + line.sku());
            }
            product.decreaseStock(line.quantity());
            order.addItem(new OrderItem(product, line.quantity()));
        }

        order = orders.save(order);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotency.save(new IdempotencyRecord(idempotencyKey, order.getId()));
        }

        outbox.publish("OrderPlaced", order.getOrderNo(), Map.of(
                "orderNo", order.getOrderNo(),
                "customerEmail", user.getEmail(),
                "total", order.getTotal(),
                "currency", order.getCurrency(),
                "itemCount", order.getItems().size()));

        return OrderDto.from(order);
    }

    @Transactional
    public OrderDto placeFromCart(User user, String idempotencyKey) {
        Cart cart = carts.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.badRequest("Cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw ApiException.badRequest("Cart is empty");
        }

        List<PlaceOrderCommand.Line> lines = cart.getItems().stream()
                .map(i -> new PlaceOrderCommand.Line(i.getProduct().getSku(), i.getQuantity()))
                .toList();

        OrderDto dto = place(new PlaceOrderCommand(lines), user, idempotencyKey);

        cart.getItems().forEach(cart::removeItem);
        carts.save(cart);
        return dto;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDto> myOrders(User user, Pageable pageable) {
        Page<Order> page = orders.findByUserId(user.getId(), pageable);
        return PageResponse.from(page, OrderDto::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDto> adminOrders(OrderStatus status, Pageable pageable) {
        Page<Order> page = status != null
                ? orders.findByStatus(status, pageable)
                : orders.findAll(pageable);
        return PageResponse.from(page, OrderDto::from);
    }

    @Transactional(readOnly = true)
    public OrderDto get(String orderNo, User requester) {
        Order order = find(orderNo);
        if (requester.getRole() != Role.ADMIN && !order.getUser().getId().equals(requester.getId())) {
            throw ApiException.forbidden("You do not have permission to view this order");
        }
        return OrderDto.from(order);
    }

    @Transactional
    public OrderDto markPaid(String orderNo, String paymentReference) {
        Order order = find(orderNo);
        order.markPaid(paymentReference);
        return OrderDto.from(order);
    }

    /** Compensates a failed payment by restoring stock and marking the order as failed. */
    @Transactional
    public OrderDto failAndRestoreStock(String orderNo) {
        Order order = find(orderNo);
        for (OrderItem item : order.getItems()) {
            item.getProduct().increaseStock(item.getQuantity());
        }
        order.markPaymentFailed();
        return OrderDto.from(order);
    }

    @Transactional
    public OrderDto cancel(String orderNo, User requester) {
        Order order = find(orderNo);
        if (requester.getRole() != Role.ADMIN && !order.getUser().getId().equals(requester.getId())) {
            throw ApiException.forbidden("You do not have permission to cancel this order");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw ApiException.conflict("Cannot cancel an order in status: " + order.getStatus());
        }
        for (OrderItem item : order.getItems()) {
            item.getProduct().increaseStock(item.getQuantity());
        }
        order.cancel();
        return OrderDto.from(order);
    }

    private Order find(String orderNo) {
        return orders.findByOrderNo(orderNo)
                .orElseThrow(() -> ApiException.notFound("Order", orderNo));
    }

    private List<PlaceOrderCommand.Line> normalize(List<PlaceOrderCommand.Line> lines) {
        if (lines == null || lines.isEmpty()) {
            throw ApiException.badRequest("Order must contain at least one line");
        }
        Map<String, Integer> merged = new LinkedHashMap<>();
        for (var line : lines) {
            if (line.quantity() <= 0) {
                throw ApiException.badRequest("Quantity must be positive: " + line.sku());
            }
            merged.merge(line.sku(), line.quantity(), Integer::sum);
        }
        List<PlaceOrderCommand.Line> normalized = new ArrayList<>();
        merged.forEach((sku, qty) -> normalized.add(new PlaceOrderCommand.Line(sku, qty)));
        // Deterministic SKU ordering reduces the chance of deadlocks between concurrent checkouts.
        normalized.sort(Comparator.comparing(PlaceOrderCommand.Line::sku));
        return normalized;
    }

    private String newOrderNo() {
        String candidate;
        do {
            candidate = "ORD-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 12).toUpperCase();
        } while (orders.existsByOrderNo(candidate));
        return candidate;
    }
}
