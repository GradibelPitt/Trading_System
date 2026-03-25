package com.exchange.order.repository;

import com.exchange.common.enums.OrderStatus;
import com.exchange.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    Page<Order> findByUserId(String userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(String userId, OrderStatus status, Pageable pageable);

    List<Order> findByUserIdAndStatusIn(String userId, List<OrderStatus> statuses);

    Optional<Order> findByIdAndUserId(String id, String userId);
}
