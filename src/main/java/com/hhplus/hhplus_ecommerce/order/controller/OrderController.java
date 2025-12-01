package com.hhplus.hhplus_ecommerce.order.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.order.application.OrderService;
import com.hhplus.hhplus_ecommerce.order.dto.request.CreateOrderFromCartRequest;
import com.hhplus.hhplus_ecommerce.order.dto.request.CreateOrderRequest;
import com.hhplus.hhplus_ecommerce.order.dto.response.OrderListResponse;
import com.hhplus.hhplus_ecommerce.order.dto.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final OrderService orderService;

    @Override
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(CreateOrderRequest request) {
        OrderResponse response = orderService.createOrderWithResponse(
                request.userId(),
                request.cartItemIds(),
                request.couponId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("주문이 생성되었습니다.", response));
    }

    @PostMapping("/cart")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrderFromCart(CreateOrderFromCartRequest request) {
        OrderResponse response = orderService.createOrderFromEntireCart(
                request.userId(),
                request.couponId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("장바구니 전체가 주문되었습니다.", response));
    }


    @Override
    public ResponseEntity<ApiResponse<OrderListResponse>> getOrders(
            Long userId, Integer page, Integer size, String status) {
        OrderListResponse response = orderService.getOrdersByUserIdWithResponse(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(Long orderId) {
        OrderResponse response = orderService.getOrderDetailResponse(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}