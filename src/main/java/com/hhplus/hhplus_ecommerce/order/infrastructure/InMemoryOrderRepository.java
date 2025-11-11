package com.hhplus.hhplus_ecommerce.order.infrastructure;

import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.order.domain.Order;
import com.hhplus.hhplus_ecommerce.order.repository.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<Long, Order> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            Order newOrder = Order.builder()
                    .id(idGenerator.getAndIncrement())
                    .userId(order.getUserId())
                    .couponId(order.getCouponId())
                    .items(order.getItems())
                    .totalAmount(order.getTotalAmount())
                    .discountAmount(order.getDiscountAmount())
                    .finalAmount(order.getFinalAmount())
                    .status(order.getStatus())
                    .createdAt(order.getCreatedAt())
                    .updatedAt(order.getUpdatedAt())
                    .build();
            store.put(newOrder.getId(), newOrder);
            return newOrder;
        } else {
            store.put(order.getId(), order);
            return order;
        }
    }

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        return store.values().stream()
                .filter(order -> userId.equals(order.getUserId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return store.values().stream()
                .filter(order -> status.equals(order.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findAll() {
        return new ArrayList<>(store.values());
    }

    public void clear() {
        store.clear();
        idGenerator.set(1);
    }
}