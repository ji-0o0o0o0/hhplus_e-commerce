package com.hhplus.hhplus_ecommerce.order.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.order.OrderType;
import com.hhplus.hhplus_ecommerce.order.dto.request.CreateOrderRequest;
import com.hhplus.hhplus_ecommerce.order.dto.response.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController implements OrderApi {

    @Override
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(CreateOrderRequest request) {
        // TODO: Service 계층으로 위임
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

    @Override
    public ResponseEntity<ApiResponse<OrderListResponse>> getOrders(
            Long userId, Integer page, Integer size, String status) {
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

    @Override
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(Long orderId) {
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