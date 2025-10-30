package com.hhplus.hhplus_ecommerce.cart.controller;

import com.hhplus.hhplus_ecommerce.cart.dto.request.AddCartItemRequest;
import com.hhplus.hhplus_ecommerce.cart.dto.response.*;
import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "장바구니 API", description = "장바구니 관리 API")
public class CartController {

    @PostMapping("/items")
    @Operation(summary = "장바구니 추가", description = "상품을 장바구니에 추가합니다")
    public ResponseEntity<ApiResponse<CartItemResponse>> addCartItem(
            @Valid @RequestBody AddCartItemRequest request
    ) {
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

    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "장바구니 삭제", description = "장바구니에서 상품을 삭제합니다")
    public ResponseEntity<Void> removeCartItem(
            @Parameter(description = "장바구니 항목 ID", example = "1", required = true)
            @PathVariable Long cartItemId
    ) {
        // Mock: 실제로는 삭제 로직 수행
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/items")
    @Operation(summary = "장바구니 조회", description = "사용자의 장바구니 목록을 조회합니다")
    public ResponseEntity<ApiResponse<CartListResponse>> getCartItems(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @RequestParam Long userId
    ) {
        // Mock 데이터
        List<CartDto> cartItems = List.of(
                new CartDto(1L, 1L, "Macbook Pro", 2, 2000000, 4000000),
                new CartDto(2L, 2L, "iPhone 12", 1, 1200000, 1200000)
        );

        CartListResponse response = new CartListResponse(cartItems, 5200000);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}