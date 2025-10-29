package com.hhplus.hhplus_ecommerce.payment.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.payment.PaymentStatus;
import com.hhplus.hhplus_ecommerce.payment.dto.request.ExecutePaymentRequest;
import com.hhplus.hhplus_ecommerce.payment.dto.response.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.hhplus.hhplus_ecommerce.payment.docs.PaymentSwaggerDocs.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "결제 실행 API", description = "결제 실행 API")
public class PaymentController {
    @PostMapping
    @Operation(summary = "결제 실행", description = "생성된 주문을 결제한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "결제 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "성공", value = PAYMENT_SUCCESS))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잔액 부족",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "잔액 부족", value = INSUFFICIENT_BALANCE))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "주문 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "주문 없음", value = ORDER_NOT_FOUND))
            )
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody ExecutePaymentRequest request
            ){
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
