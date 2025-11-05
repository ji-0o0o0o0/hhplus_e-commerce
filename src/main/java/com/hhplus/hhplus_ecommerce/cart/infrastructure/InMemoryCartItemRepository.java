package com.hhplus.hhplus_ecommerce.cart.infrastructure;

import com.hhplus.hhplus_ecommerce.cart.domain.CartItem;
import com.hhplus.hhplus_ecommerce.cart.repository.CartItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryCartItemRepository implements CartItemRepository {

    private final Map<Long, CartItem> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public CartItem save(CartItem cartItem) {
        if (cartItem.getId() == null) {
            CartItem newItem = CartItem.builder()
                    .id(idGenerator.getAndIncrement())
                    .userId(cartItem.getUserId())
                    .productId(cartItem.getProductId())
                    .quantity(cartItem.getQuantity())
                    .createdAt(cartItem.getCreatedAt())
                    .build();
            store.put(newItem.getId(), newItem);
            return newItem;
        } else {
            store.put(cartItem.getId(), cartItem);
            return cartItem;
        }
    }

    @Override
    public Optional<CartItem> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<CartItem> findByUserId(Long userId) {
        return store.values().stream()
                .filter(item -> userId.equals(item.getUserId()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId) {
        return store.values().stream()
                .filter(item -> userId.equals(item.getUserId()) && productId.equals(item.getProductId()))
                .findFirst();
    }

    @Override
    public void delete(Long id) {
        store.remove(id);
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        List<Long> toDelete = store.values().stream()
                .filter(item -> userId.equals(item.getUserId()))
                .map(CartItem::getId)
                .collect(Collectors.toList());

        toDelete.forEach(store::remove);
    }

    public void clear() {
        store.clear();
        idGenerator.set(1);
    }
}