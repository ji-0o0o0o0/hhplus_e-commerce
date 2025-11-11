package com.hhplus.hhplus_ecommerce.cart.repository;

import com.hhplus.hhplus_ecommerce.cart.domain.CartItem;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository {

    CartItem save(CartItem cartItem);
    Optional<CartItem> findById(Long id);
    List<CartItem> findByUserId(Long userId);
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);
    void delete(Long id);
    void deleteAllByUserId(Long userId);
}