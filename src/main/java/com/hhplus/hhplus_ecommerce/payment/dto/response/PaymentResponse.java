package com.hhplus.hhplus_ecommerce.payment.dto.response;

import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.payment.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "결제 응답")
public class PaymentResponse {

    @Schema(description = "결제 ID", example = "1")
    private Long paymentId;

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "최종 결제 금액", example = "50000")
    private Integer finalAmount;

    @Schema(description = "결제 후 잔액", example = "50000")
    private Integer balanceAfter;

    @Schema(description = "결제 상태 (COMPLETED, FAILED)", example = "COMPLETED")
    private PaymentStatus status;

    @Schema(description = "결제 시간", example = "2025-10-29T10:00:00")
    private LocalDateTime paidAt;
}