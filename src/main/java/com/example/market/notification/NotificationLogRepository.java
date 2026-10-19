package com.example.market.notification;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for sent notification records.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public interface NotificationLogRepository extends JpaRepository<NotificationLog, String> { }
