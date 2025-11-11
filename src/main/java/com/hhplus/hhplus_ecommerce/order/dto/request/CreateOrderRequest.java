package com.hhplus.hhplus_ecommerce.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "주문 생성 요청")
public class CreateOrderRequest {

    @NotNull(message = "사용자 ID는 필수입니다")
    @Schema(description = "사용자 ID", example = "1", required = true)
    private Long userId;

    @Schema(description = "쿠폰 ID (선택)", example = "1")
    private Long couponId;
}