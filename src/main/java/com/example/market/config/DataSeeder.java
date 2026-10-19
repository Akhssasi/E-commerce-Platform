package com.example.market.config;

import com.example.market.auth.Role;
import com.example.market.auth.User;
import com.example.market.auth.UserRepository;
import com.example.market.catalog.Category;
import com.example.market.catalog.CategoryRepository;
import com.example.market.catalog.Product;
import com.example.market.catalog.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds the initial admin account and a small demo catalog on first startup.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
@Component
@ConditionalOnProperty(name = "market.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(UserRepository users, CategoryRepository categories, ProductRepository products,
                      PasswordEncoder passwordEncoder,
                      @Value("${market.seed.admin-email}") String adminEmail,
                      @Value("${market.seed.admin-password}") String adminPassword) {
        this.users = users;
        this.categories = categories;
        this.products = products;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedCatalog();
    }

    private void seedAdmin() {
        if (users.existsByEmail(adminEmail)) return;
        users.save(new User(adminEmail, "Store Administrator",
                passwordEncoder.encode(adminPassword), Role.ADMIN));
        log.info("Created admin account: {} (change the password in production)", adminEmail);
    }

    private void seedCatalog() {
        if (products.count() > 0) return;

        Category electronics = categories.findByName("Electronics")
                .orElseGet(() -> categories.save(new Category("Electronics")));
        Category accessories = categories.findByName("Accessories")
                .orElseGet(() -> categories.save(new Category("Accessories")));

        List<Product> demo = List.of(
                new Product(electronics, "Smartphone with Pro Camera", "PHONE-001", new BigDecimal("499.00"), 25),
                new Product(electronics, "Business Laptop", "LAPTOP-001", new BigDecimal("899.00"), 10),
                new Product(electronics, "Smart Watch", "WATCH-001", new BigDecimal("199.00"), 40),
                new Product(accessories, "Wireless Headphones", "HEADPHONE-001", new BigDecimal("79.00"), 60),
                new Product(accessories, "65W Fast Charger", "CHARGER-001", new BigDecimal("39.00"), 100),
                new Product(accessories, "Durable USB-C Cable", "CABLE-001", new BigDecimal("12.00"), 200)
        );
        products.saveAll(demo);
        log.info("Seeded a demo catalog with {} products", demo.size());
    }
}
