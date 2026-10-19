package com.example.market.order;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for idempotency records.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> { }
