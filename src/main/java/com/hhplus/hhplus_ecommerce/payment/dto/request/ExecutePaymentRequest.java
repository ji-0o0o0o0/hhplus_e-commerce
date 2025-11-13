package com.hhplus.hhplus_ecommerce.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "결제 실행 요청")
public record ExecutePaymentRequest(

    @NotNull(message = "사용자 ID는 필수입니다")
    @Schema(description = "사용자 ID", example = "1", required = true)
    Long userId,

    @NotNull(message = "주문 ID는 필수입니다")
    @Schema(description = "주문 ID", example = "1", required = true)
    Long orderId
){}