package com.hhplus.hhplus_ecommerce.order.repository;

import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);
    List<Order> findByStatus(OrderStatus status);
}