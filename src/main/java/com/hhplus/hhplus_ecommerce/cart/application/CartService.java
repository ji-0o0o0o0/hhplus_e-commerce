package com.hhplus.hhplus_ecommerce.cart.application;

import com.hhplus.hhplus_ecommerce.cart.domain.CartItem;
import com.hhplus.hhplus_ecommerce.cart.repository.CartItemRepository;
import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartItem addCartItem(Long userId, Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.hasSufficientStock(quantity)) {
            throw new BusinessException(ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
        }

        Optional<CartItem> existingItem = cartItemRepository.findByUserIdAndProductId(userId, productId);

        if (existingItem.isPresent()) {
            CartItem cartItem = existingItem.get();
            cartItem.increaseQuantity(quantity);
            return cartItemRepository.save(cartItem);
        } else {
            CartItem newCartItem = CartItem.create(userId, productId, quantity);
            return cartItemRepository.save(newCartItem);
        }
    }

    public List<CartItem> getCartItems(Long userId) {
        return cartItemRepository.findByUserId(userId);
    }

    public void removeCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!userId.equals(cartItem.getUserId())) {
            throw new BusinessException(ErrorCode.CART_ITEM_ACCESS_DENIED);
        }

        cartItemRepository.deleteById(cartItemId);
    }

    public CartItem decreaseCartItemQuantity(Long userId, Long cartItemId, Integer quantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!userId.equals(cartItem.getUserId())) {
            throw new BusinessException(ErrorCode.CART_ITEM_ACCESS_DENIED);
        }

        cartItem.decreaseQuantity(quantity);

        if (cartItem.isQuantityZeroOrLess()) {
            cartItemRepository.deleteById(cartItemId);
            return null;
        } else {
            return cartItemRepository.save(cartItem);
        }

    }


    public void clearCart(Long userId) {
        cartItemRepository.deleteAllByUserId(userId);
    }
}
