package com.hhplus.hhplus_ecommerce.order.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.order.OrderType;
import com.hhplus.hhplus_ecommerce.order.dto.request.CreateOrderRequest;
import com.hhplus.hhplus_ecommerce.order.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.hhplus.hhplus_ecommerce.order.docs.OrderSwaggerDocs.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "주문 API", description = "주문 관리 API")
public class OrderController {

    @PostMapping
    @Operation(summary = "주문 생성", description = "장바구니 또는 즉시 구매로 주문을 생성합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "주문 생성 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "성공", value = CREATE_ORDER_SUCCESS))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "상품 목록 없음", value = DIRECT_ORDER_WITHOUT_ITEMS),
                                    @ExampleObject(name = "재고 부족", value = PRODUCT_OUT_OF_STOCK)
                            })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "쿠폰 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "쿠폰 없음", value = COUPON_NOT_FOUND))
            )
    })
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        // OrderType 검증
        if (request.getOrderType() == OrderType.CART) {
            // 장바구니 주문
            // Mock: 실제로는 CartService에서 장바구니 조회
            // 여기서는 항상 성공한다고 가정

        } else if (request.getOrderType() == OrderType.DIRECT) {
            // 즉시 구매 - items 필수
            if (request.getItems() == null || request.getItems().isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(ApiResponse.error(400, "즉시 구매 시 상품 목록은 필수입니다."));
            }

            // Mock: 특정 상품 ID는 재고 부족으로 처리
            boolean outOfStock = request.getItems().stream()
                    .anyMatch(item -> item.getProductId() == 999L);
            if (outOfStock) {
                return ResponseEntity
                        .badRequest()
                        .body(ApiResponse.error(400, "상품의 재고가 부족합니다."));
            }
        }

        // Mock: 쿠폰 검증
        if (request.getCouponId() != null && request.getCouponId() == 999L) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "쿠폰을 찾을 수 없습니다."));
        }

        // Mock 데이터
        List<OrderItemDto> orderItems = List.of(
                new OrderItemDto(1L, 1L, "Macbook Pro", 1, 2000000, 2000000)
        );

        OrderResponse response = new OrderResponse(
                1L,
                request.getUserId(),
                2000000,
                200000,
                1800000,
                OrderStatus.PENDING,
                orderItems,
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("주문이 생성되었습니다.", response));
    }

    @GetMapping
    @Operation(summary = "주문 목록 조회", description = "사용자의 주문 목록을 조회합니다")
    public ResponseEntity<ApiResponse<OrderListResponse>> getOrders(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @RequestParam Long userId,

            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") Integer size,

            @Parameter(description = "주문 상태 (PENDING, COMPLETED, CANCELLED)")
            @RequestParam(required = false) String status
    ) {
        // Mock 데이터
        List<OrderListDto> orders = List.of(
                new OrderListDto(1L, 50000, OrderStatus.COMPLETED, LocalDateTime.now().minusDays(1)),
                new OrderListDto(2L, 30000, OrderStatus.PENDING, LocalDateTime.now())
        );

        OrderListResponse response = new OrderListResponse(
                orders,
                10L,
                1,
                page,
                size
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "주문 상세 조회", description = "특정 주문의 상세 정보를 조회합니다")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @Parameter(description = "주문 ID", example = "1", required = true)
            @PathVariable Long orderId
    ) {
        // Mock 데이터
        List<OrderItemDto> orderItems = List.of(
                new OrderItemDto(1L, 1L, "Macbook Pro", 1, 2000000, 2000000)
        );

        OrderResponse response = new OrderResponse(
                orderId,
                1L,
                2000000,
                200000,
                1800000,
                OrderStatus.COMPLETED,
                orderItems,
                LocalDateTime.now().minusDays(1)
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}