package com.hhplus.hhplus_ecommerce.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "결제 응답")
public record PaymentResponse(

    @Schema(description = "주문 ID", example = "1")
     Long orderId,

    @Schema(description = "사용자 ID", example = "1")
     Long userId,

    @Schema(description = "최종 결제 금액", example = "50000")
     Integer finalAmount,

    @Schema(description = "결제 후 포인트 잔액", example = "50000")
     Integer pointBalanceAfter,

    @Schema(description = "결제 시간", example = "2025-10-29T10:00:00")
     LocalDateTime paidAt
){}