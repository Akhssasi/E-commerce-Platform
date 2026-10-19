package com.example.market.order;

import com.example.market.TestcontainersConfiguration;
import com.example.market.auth.JwtService;
import com.example.market.auth.Role;
import com.example.market.auth.User;
import com.example.market.auth.UserRepository;
import com.example.market.catalog.Category;
import com.example.market.catalog.CategoryRepository;
import com.example.market.catalog.Product;
import com.example.market.catalog.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests covering atomic checkout, idempotency, stock compensation and the end-to-end HTTP flow.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OrderPlacementIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired OrderFacade orderFacade;
    @Autowired OrderService orderService;
    @Autowired JwtService jwtService;

    @Test
    void idempotentPurchaseChargesStockOnlyOnce() {
        User user = newUser();
        Product product = newProduct(new BigDecimal("20.00"), 10);

        var command = new PlaceOrderCommand(List.of(
                new PlaceOrderCommand.Line(product.getSku(), 3)));

        String key = UUID.randomUUID().toString();
        OrderDto first = orderFacade.purchase(command, user, key);
        OrderDto second = orderFacade.purchase(command, user, key);

        assertThat(first.orderNo()).isEqualTo(second.orderNo());
        assertThat(first.status()).isEqualTo(OrderStatus.PAID.name());
        assertThat(first.total()).isEqualByComparingTo("60.00");

        Product refreshed = products.findBySku(product.getSku()).orElseThrow();
        assertThat(refreshed.getStock()).isEqualTo(7);
    }

    @Test
    void failingPaymentRestoresStockAndMarksFailed() {
        User user = newUser();
        Product product = newProduct(new BigDecimal("15.00"), 5);

        OrderDto placed = orderService.place(new PlaceOrderCommand(
                List.of(new PlaceOrderCommand.Line(product.getSku(), 2))), user, "key-" + UUID.randomUUID());
        assertThat(products.findBySku(product.getSku()).orElseThrow().getStock()).isEqualTo(3);

        OrderDto failed = orderService.failAndRestoreStock(placed.orderNo());
        assertThat(failed.status()).isEqualTo(OrderStatus.PAYMENT_FAILED.name());
        assertThat(products.findBySku(product.getSku()).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    void purchaseWithInsufficientStockConflictsAndKeepsStock() {
        User user = newUser();
        Product product = newProduct(new BigDecimal("10.00"), 1);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                orderFacade.purchase(new PlaceOrderCommand(List.of(
                        new PlaceOrderCommand.Line(product.getSku(), 5))), user, UUID.randomUUID().toString()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(products.findBySku(product.getSku()).orElseThrow().getStock()).isEqualTo(1);
    }

    @Test
    void endToEndPurchaseOverHttp() throws Exception {
        String email = "buyer-" + UUID.randomUUID() + "@example.com";
        Product product = newProduct(new BigDecimal("30.00"), 4);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","fullName":"Buyer","password":"Secret@123"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        String login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Secret@123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(login).get("accessToken").asText();

        String body = objectMapper.writeValueAsString(new PlaceOrderCommand(List.of(
                new PlaceOrderCommand.Line(product.getSku(), 2))));

        String response = mockMvc.perform(post("/api/orders/purchase")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "e2e-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andReturn().getResponse().getContentAsString();

        JsonNode order = objectMapper.readTree(response);
        String orderNo = order.get("orderNo").asText();

        mockMvc.perform(get("/api/orders/{orderNo}", orderNo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNo").value(orderNo));
    }

    @Test
    void customerCannotReadSomeoneElsesOrder() {
        User owner = newUser();
        Product product = newProduct(new BigDecimal("5.00"), 10);
        OrderDto placed = orderFacade.purchase(new PlaceOrderCommand(List.of(
                new PlaceOrderCommand.Line(product.getSku(), 1))), owner, UUID.randomUUID().toString());

        User stranger = newUser();
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                orderService.get(placed.orderNo(), stranger))
                .isInstanceOf(com.example.market.common.ApiException.class)
                .hasMessageContaining("permission");
    }

    private User newUser() {
        return users.save(new User("buyer-" + UUID.randomUUID() + "@example.com",
                "Buyer", "{noop}ignored", Role.CUSTOMER));
    }

    private Product newProduct(BigDecimal price, int stock) {
        Category category = categories.save(new Category("Category-" + UUID.randomUUID()));
        String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return products.save(new Product(category, "Product " + sku, sku, price, stock));
    }
}
