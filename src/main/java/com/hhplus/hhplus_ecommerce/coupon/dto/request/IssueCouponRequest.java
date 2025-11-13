package com.hhplus.hhplus_ecommerce.coupon.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "쿠폰 발급 요청")
public record IssueCouponRequest (
    @NotNull(message = "사용자 ID는 필수입니다")
    @Schema(description = "사용자 ID", example = "1", required = true)
     Long userId,

    @NotNull(message = "쿠폰 ID는 필수입니다")
    @Schema(description = "쿠폰 ID", example = "1", required = true)
     Long couponId
){}
