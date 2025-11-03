package com.hhplus.hhplus_ecommerce.cart.controller;

import com.hhplus.hhplus_ecommerce.cart.dto.request.AddCartItemRequest;
import com.hhplus.hhplus_ecommerce.cart.dto.response.*;
import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 장바구니 API 구현체
 * - CartApi 인터페이스를 구현
 * - 실제 장바구니 로직 처리 (현재는 Mock 데이터)
 */
@RestController
@RequestMapping("/api/cart")
public class CartController implements CartApi {

    @Override
    public ResponseEntity<ApiResponse<CartItemResponse>> addCartItem(AddCartItemRequest request) {
        // Mock 데이터
        CartItemResponse response = new CartItemResponse(
                1L,
                request.getUserId(),
                request.getProductId(),
                "Macbook Pro",
                request.getQuantity(),
                2000000
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("장바구니에 추가되었습니다.", response));
    }

    @Override
    public ResponseEntity<Void> removeCartItem(Long cartItemId) {
        // Mock: 실제로는 삭제 로직 수행
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ApiResponse<CartListResponse>> getCartItems(Long userId) {
        // Mock 데이터
        List<CartDto> cartItems = List.of(
                new CartDto(1L, 1L, "Macbook Pro", 2, 2000000, 4000000),
                new CartDto(2L, 2L, "iPhone 12", 1, 1200000, 1200000)
        );

        CartListResponse response = new CartListResponse(cartItems, 5200000);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}