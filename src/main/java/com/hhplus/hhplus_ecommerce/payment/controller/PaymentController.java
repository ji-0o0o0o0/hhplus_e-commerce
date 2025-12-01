package com.hhplus.hhplus_ecommerce.payment.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.payment.application.PaymentService;
import com.hhplus.hhplus_ecommerce.payment.dto.request.ExecutePaymentRequest;
import com.hhplus.hhplus_ecommerce.payment.dto.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController implements PaymentApi {

    private final PaymentService paymentService;


    @Override
    public ResponseEntity<ApiResponse<PaymentResponse>> executePayment(ExecutePaymentRequest request) {

        PaymentResponse response = paymentService.executePaymentWithResponse(request.userId(),request.orderId());
        return ResponseEntity.ok(ApiResponse.success(response));

    }
}

