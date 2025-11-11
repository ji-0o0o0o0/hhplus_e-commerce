package com.hhplus.hhplus_ecommerce.cart.controller;

import com.hhplus.hhplus_ecommerce.cart.dto.request.AddCartItemRequest;
import com.hhplus.hhplus_ecommerce.cart.dto.response.CartItemResponse;
import com.hhplus.hhplus_ecommerce.cart.dto.response.CartListResponse;
import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 장바구니 API 명세
 * - Swagger 문서화를 위한 인터페이스
 * - API 명세와 실제 구현을 분리
 */
@Tag(name = "장바구니 API", description = "장바구니 관리 API")
public interface CartApi {

    /**
     * 장바구니에 상품 추가
     */
    @PostMapping("/items")
    @Operation(summary = "장바구니에 상품 추가", description = "사용자의 장바구니에 상품을 추가합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "장바구니 추가 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": true,
                                  "message": "장바구니에 추가되었습니다.",
                                  "data": {
                                    "cartItemId": 1,
                                    "userId": 1,
                                    "productId": 1,
                                    "productName": "Macbook Pro",
                                    "quantity": 1,
                                    "price": 2000000
                                  }
                                }
                                """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "상품을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": false,
                                  "code": 404,
                                  "message": "상품을 찾을 수 없습니다."
                                }
                                """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "재고 부족",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": false,
                                  "code": 400,
                                  "message": "재고가 부족합니다."
                                }
                                """))
            )
    })
    ResponseEntity<ApiResponse<CartItemResponse>> addCartItem(
            @Valid @RequestBody AddCartItemRequest request
    );

    /**
     * 장바구니에서 상품 삭제
     */
    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "장바구니에서 상품 삭제", description = "장바구니에서 특정 상품을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "장바구니 항목을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": false,
                                  "code": 404,
                                  "message": "장바구니 항목을 찾을 수 없습니다."
                                }
                                """))
            )
    })
    ResponseEntity<Void> removeCartItem(
            @Parameter(description = "장바구니 항목 ID", example = "1", required = true)
            @PathVariable Long cartItemId
    );

    /**
     * 장바구니 조회
     */
    @GetMapping("/items")
    @Operation(summary = "장바구니 조회", description = "사용자의 장바구니 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": true,
                                  "data": {
                                    "items": [
                                      {
                                        "cartItemId": 1,
                                        "productId": 1,
                                        "productName": "Macbook Pro",
                                        "quantity": 2,
                                        "unitPrice": 2000000,
                                        "subtotal": 4000000
                                      }
                                    ],
                                    "totalAmount": 5200000
                                  }
                                }
                                """))
            )
    })
    ResponseEntity<ApiResponse<CartListResponse>> getCartItems(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @RequestParam Long userId
    );
}