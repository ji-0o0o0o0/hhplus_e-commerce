package com.hhplus.hhplus_ecommerce.payment.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.payment.PaymentStatus;
import com.hhplus.hhplus_ecommerce.payment.dto.request.ExecutePaymentRequest;
import com.hhplus.hhplus_ecommerce.payment.dto.response.PaymentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/payments")
public class PaymentController implements PaymentApi {


    @Override
    public ResponseEntity<ApiResponse<PaymentResponse>> executePayment(ExecutePaymentRequest request) {

        // Mock: 특정 주문 ID는 없는 것으로 처리
        if (request.getOrderId() == 999L) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "주문을 찾을 수 없습니다."));
        }

        // Mock: 특정 사용자 ID는 잔액 부족으로 처리
        if (request.getUserId() == 999L) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(400, "잔액이 부족합니다."));
        }

        PaymentResponse response = new PaymentResponse(
                1L,
                request.getOrderId(),
                request.getUserId(),
                50000,
                50000,
                PaymentStatus.COMPLETED,
                LocalDateTime.now()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

