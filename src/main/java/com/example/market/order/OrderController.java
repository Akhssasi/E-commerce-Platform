package com.example.market.order;

import com.example.market.common.PageResponse;
import com.example.market.common.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing purchase and order tracking endpoints for customers.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Purchase and track orders")
public class OrderController {

    private final OrderFacade facade;
    private final OrderService orderService;

    public OrderController(OrderFacade facade, OrderService orderService) {
        this.facade = facade;
        this.orderService = orderService;
    }

    @PostMapping("/purchase")
    @Operation(summary = "Place a purchase (supports the Idempotency-Key header)")
    public ResponseEntity<OrderDto> purchase(
            @Valid @RequestBody PlaceOrderCommand cmd,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        OrderDto dto = facade.purchase(cmd, SecurityUtils.currentUser(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/from-cart")
    @Operation(summary = "Purchase everything in the cart, then clear it")
    public ResponseEntity<OrderDto> purchaseFromCart(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        OrderDto dto = facade.purchaseFromCart(SecurityUtils.currentUser(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    @Operation(summary = "List my orders (paginated)")
    public PageResponse<OrderDto> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderService.myOrders(SecurityUtils.currentUser(), pageable(page, size));
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "Get order details (owner or ADMIN)")
    public OrderDto byOrderNo(@PathVariable String orderNo) {
        return orderService.get(orderNo, SecurityUtils.currentUser());
    }

    @PostMapping("/{orderNo}/cancel")
    @Operation(summary = "Cancel an order before payment")
    public OrderDto cancel(@PathVariable String orderNo) {
        return orderService.cancel(orderNo, SecurityUtils.currentUser());
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
