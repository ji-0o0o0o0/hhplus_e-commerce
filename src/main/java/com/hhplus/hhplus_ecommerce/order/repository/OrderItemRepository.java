package com.hhplus.hhplus_ecommerce.order.repository;

import com.hhplus.hhplus_ecommerce.order.domain.OrderItem;

import java.util.List;

public interface OrderItemRepository {

    OrderItem save(OrderItem orderItem);
    List<OrderItem> findByOrderId(Long orderId);
    List<OrderItem> saveAll(List<OrderItem> orderItems);
}