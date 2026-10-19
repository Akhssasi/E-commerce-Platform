package com.example.market.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data repository for persisted users.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
