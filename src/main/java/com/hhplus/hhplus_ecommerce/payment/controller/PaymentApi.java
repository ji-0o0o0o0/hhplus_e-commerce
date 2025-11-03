package com.hhplus.hhplus_ecommerce.payment.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.payment.dto.request.ExecutePaymentRequest;
import com.hhplus.hhplus_ecommerce.payment.dto.response.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 결제 API 명세
 * - Swagger 문서화를 위한 인터페이스
 * - API 명세와 실제 구현을 분리
 */
@Tag(name = "결제 API", description = "주문 결제 실행 API")
public interface PaymentApi {

    /**
     * 결제 실행
     */
    @PostMapping
    @Operation(summary = "결제 실행", description = "생성된 주문에 대해 결제를 실행합니다. 포인트를 차감하고 결제 내역을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "결제 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": true,
                                  "data": {
                                    "paymentId": 1,
                                    "orderId": 1,
                                    "userId": 1,
                                    "finalAmount": 50000,
                                    "balanceAfter": 50000,
                                    "status": "COMPLETED",
                                    "paidAt": "2024-11-03T18:00:00"
                                  }
                                }
                                """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잔액 부족",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": false,
                                  "code": 400,
                                  "message": "잔액이 부족합니다."
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 결제된 주문",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": false,
                                  "code": 409,
                                  "message": "이미 결제된 주문입니다."
                                }
                                """))
            )
    })
    ResponseEntity<ApiResponse<PaymentResponse>> executePayment(
            @Valid @RequestBody ExecutePaymentRequest request
    );
}