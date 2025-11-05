package com.hhplus.hhplus_ecommerce.order.infrastructure;

import com.hhplus.hhplus_ecommerce.order.domain.OrderItem;
import com.hhplus.hhplus_ecommerce.order.repository.OrderItemRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryOrderItemRepository implements OrderItemRepository {

    private final Map<Long, OrderItem> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public OrderItem save(OrderItem orderItem) {
        if (orderItem.getId() == null) {
            OrderItem newItem = OrderItem.builder()
                    .id(idGenerator.getAndIncrement())
                    .orderId(orderItem.getOrderId())
                    .productId(orderItem.getProductId())
                    .productName(orderItem.getProductName())
                    .unitPrice(orderItem.getUnitPrice())
                    .quantity(orderItem.getQuantity())
                    .subtotal(orderItem.getSubtotal())
                    .build();
            store.put(newItem.getId(), newItem);
            return newItem;
        } else {
            store.put(orderItem.getId(), orderItem);
            return orderItem;
        }
    }

    @Override
    public List<OrderItem> findByOrderId(Long orderId) {
        return store.values().stream()
                .filter(item -> orderId.equals(item.getOrderId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderItem> saveAll(List<OrderItem> orderItems) {
        List<OrderItem> savedItems = new ArrayList<>();
        for (OrderItem item : orderItems) {
            savedItems.add(save(item));
        }
        return savedItems;
    }

    public void clear() {
        store.clear();
        idGenerator.set(1);
    }
}