package com.hhplus.hhplus_ecommerce.order.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.order.OrderStatus;
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
        // TODO: OrderFacade로 위임 예정
        // 장바구니에서 상품 조회 → 재고 확인 → 쿠폰 적용 → 주문 생성

        // Mock: 쿠폰 검증
        if (request.couponId() != null && request.couponId() == 999L) {
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
                request.userId(),
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