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

    // 장바구니 추가
    public CartItem addCartItem(Long userId, Long productId, Integer quantity) {
        // 1. 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 2. 재고 확인
        if (product.getStock() < quantity) {
            throw new BusinessException(ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
        }

        // 3. 이미 장바구니에 있는지 확인 (있으면 수량 증가, 없으면 새로 생성)
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

    //장바구니 조회
    public List<CartItem> getCartItems(Long userId) {
        return cartItemRepository.findByUserId(userId);
    }

    //장바구니 삭제 1. 특정 항목 완전 삭제 (userId 검증 추가)
    public void removeCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        // 본인 확인
        if (!userId.equals(cartItem.getUserId())) {
            throw new BusinessException(ErrorCode.CART_ITEM_ACCESS_DENIED);
        }

        cartItemRepository.delete(cartItemId);
    }

    //장바구니 삭제 2. 수량만 감소 (수량 1개씩 빼기)
    public CartItem decreaseCartItemQuantity(Long userId, Long cartItemId, Integer quantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!userId.equals(cartItem.getUserId())) {
            throw new BusinessException(ErrorCode.CART_ITEM_ACCESS_DENIED);
        }

        // 수량 감소
        int newQuantity = cartItem.getQuantity() - quantity;

        if (newQuantity <= 0) {
            // 수량이 0 이하면 삭제
            cartItemRepository.delete(cartItemId);
            return null;
        } else {
            // 수량 업데이트
            cartItem.updateQuantity(newQuantity);
            return cartItemRepository.save(cartItem);
        }
    }


    //장바구니 전체 삭제
    public void clearCart(Long userId) {
        cartItemRepository.deleteAllByUserId(userId);
    }
}
