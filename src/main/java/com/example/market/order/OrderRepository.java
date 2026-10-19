package com.example.market.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data repository for orders, including ownership and status queries.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("select o from Order o join fetch o.user where o.orderNo = :orderNo")
    Optional<Order> findByOrderNo(@Param("orderNo") String orderNo);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    boolean existsByOrderNo(String orderNo);
}
