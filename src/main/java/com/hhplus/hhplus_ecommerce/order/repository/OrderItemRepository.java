package com.hhplus.hhplus_ecommerce.order.repository;

import com.hhplus.hhplus_ecommerce.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);
}