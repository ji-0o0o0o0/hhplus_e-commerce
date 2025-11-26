package com.hhplus.hhplus_ecommerce.cart.controller;

import com.hhplus.hhplus_ecommerce.cart.application.CartService;
import com.hhplus.hhplus_ecommerce.cart.dto.request.AddCartItemRequest;
import com.hhplus.hhplus_ecommerce.cart.dto.response.CartItemResponse;
import com.hhplus.hhplus_ecommerce.cart.dto.response.CartListResponse;
import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController implements CartApi {

    private final CartService cartService;

    @Override
    public ResponseEntity<ApiResponse<CartItemResponse>> addCartItem(AddCartItemRequest request) {
        CartItemResponse response = cartService.addCartItemWithResponse(
                request.userId(),
                request.productId(),
                request.quantity()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("장바구니에 추가되었습니다.", response));
    }

    @Override
    public ResponseEntity<Void> removeCartItem(Long userId, Long cartItemId) {
        cartService.removeCartItem(userId,cartItemId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ApiResponse<CartListResponse>> getCartItems(Long userId) {
        CartListResponse response = cartService.getCartItemsWithDetails(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}