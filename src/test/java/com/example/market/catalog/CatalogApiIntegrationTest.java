package com.example.market.catalog;

import com.example.market.TestcontainersConfiguration;
import com.example.market.auth.JwtService;
import com.example.market.auth.Role;
import com.example.market.auth.User;
import com.example.market.auth.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests covering public catalog access, role enforcement and admin product updates.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CatalogApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @Test
    void anonymousCanBrowseCatalog() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void creatingProductRequiresAdmin() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":1,"name":"x","sku":"X-1","price":10.00,"stock":1}
                                """))
                .andExpect(status().isUnauthorized());

        User customer = users.save(new User(
                "cust-" + UUID.randomUUID() + "@example.com", "Customer",
                passwordEncoder.encode("Secret@123"), Role.CUSTOMER));
        String customerToken = jwtService.generateToken(customer);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":1,"name":"x","sku":"X-1","price":10.00,"stock":1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAndUpdateProductStock() throws Exception {
        User admin = users.save(new User(
                "admin-" + UUID.randomUUID() + "@example.com", "Administrator",
                passwordEncoder.encode("Secret@123"), Role.ADMIN));
        String token = jwtService.generateToken(admin);
        Category category = categories.save(new Category("Category-" + UUID.randomUUID()));
        String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":%d,"name":"New Product","sku":"%s","price":25.00,"stock":7}
                                """.formatted(category.getId(), sku)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value(sku))
                .andExpect(jsonPath("$.stock").value(7));

        mockMvc.perform(patch("/api/products/{sku}/stock", sku)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stock": 15}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(15));

        mockMvc.perform(get("/api/products/{sku}", sku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(25.00));
    }

    @Test
    void unknownSkuReturns404() throws Exception {
        mockMvc.perform(get("/api/products/DOES-NOT-EXIST"))
                .andExpect(status().isNotFound());
    }
}
