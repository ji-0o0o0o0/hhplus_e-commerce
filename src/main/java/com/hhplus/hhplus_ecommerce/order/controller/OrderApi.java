package com.hhplus.hhplus_ecommerce.order.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.order.dto.request.CreateOrderRequest;
import com.hhplus.hhplus_ecommerce.order.dto.response.OrderListResponse;
import com.hhplus.hhplus_ecommerce.order.dto.response.OrderResponse;
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
 * 주문 API 명세
 * - Swagger 문서화를 위한 인터페이스
 * - API 명세와 실제 구현을 분리
 */
@Tag(name = "주문 API", description = "주문 생성 및 조회 API")
public interface OrderApi {

    /**
     * 주문 생성
     */
    @PostMapping
    @Operation(summary = "주문 생성", description = "장바구니 또는 즉시 구매로 주문을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "주문 생성 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": true,
                                  "message": "주문이 생성되었습니다.",
                                  "data": {
                                    "orderId": 1,
                                    "userId": 1,
                                    "totalAmount": 2000000,
                                    "discountAmount": 200000,
                                    "finalAmount": 1800000,
                                    "status": "PENDING",
                                    "orderItems": [
                                      {
                                        "orderItemId": 1,
                                        "productId": 1,
                                        "productName": "Macbook Pro",
                                        "quantity": 1,
                                        "unitPrice": 2000000,
                                        "subtotal": 2000000
                                      }
                                    ],
                                    "createdAt": "2024-11-03T18:00:00"
                                  }
                                }
                                """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "상품 목록 없음", value = """
                                        {
                                          "success": false,
                                          "code": 400,
                                          "message": "즉시 구매 시 상품 목록은 필수입니다."
                                        }
                                        """),
                                    @ExampleObject(name = "재고 부족", value = """
                                        {
                                          "success": false,
                                          "code": 400,
                                          "message": "상품의 재고가 부족합니다."
                                        }
                                        """)
                            })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "쿠폰을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": false,
                                  "code": 404,
                                  "message": "쿠폰을 찾을 수 없습니다."
                                }
                                """))
            )
    })
    ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    );

    /**
     * 주문 목록 조회
     */
    @GetMapping
    @Operation(summary = "주문 목록 조회", description = "사용자의 주문 목록을 페이징하여 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": true,
                                  "data": {
                                    "orders": [
                                      {
                                        "orderId": 1,
                                        "finalAmount": 50000,
                                        "status": "COMPLETED",
                                        "createdAt": "2024-11-02T18:00:00"
                                      }
                                    ],
                                    "totalElements": 10,
                                    "totalPages": 1,
                                    "currentPage": 0,
                                    "pageSize": 10
                                  }
                                }
                                """))
            )
    })
    ResponseEntity<ApiResponse<OrderListResponse>> getOrders(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @RequestParam Long userId,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") Integer size,

            @Parameter(description = "주문 상태 필터 (PENDING, COMPLETED, CANCELLED)")
            @RequestParam(required = false) String status
    );

    /**
     * 주문 상세 조회
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "주문 상세 조회", description = "특정 주문의 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": true,
                                  "data": {
                                    "orderId": 1,
                                    "userId": 1,
                                    "totalAmount": 2000000,
                                    "discountAmount": 200000,
                                    "finalAmount": 1800000,
                                    "status": "COMPLETED",
                                    "orderItems": [
                                      {
                                        "orderItemId": 1,
                                        "productId": 1,
                                        "productName": "Macbook Pro",
                                        "quantity": 1,
                                        "unitPrice": 2000000,
                                        "subtotal": 2000000
                                      }
                                    ],
                                    "createdAt": "2024-11-02T18:00:00"
                                  }
                                }
                                """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "주문을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": false,
                                  "code": 404,
                                  "message": "주문을 찾을 수 없습니다."
                                }
                                """))
            )
    })
    ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @Parameter(description = "주문 ID", example = "1", required = true)
            @PathVariable Long orderId
    );
}