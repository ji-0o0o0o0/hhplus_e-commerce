package com.hhplus.hhplus_ecommerce.coupon.dto.response;

import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "사용자 쿠폰 정보")
public record UserCouponDto (

    @Schema(description = "사용자 쿠폰 ID", example = "1")
     Long userCouponId,

    @Schema(description = "쿠폰 ID", example = "1")
     Long couponId,

    @Schema(description = "쿠폰명", example = "20% 할인 쿠폰")
     String couponName,

    @Schema(description = "할인율 (%)", example = "20")
     Integer discountRate,

    @Schema(description = "쿠폰 상태 (AVAILABLE, USED, EXPIRED)", example = "AVAILABLE")
     CouponStatus status,

    @Schema(description = "발급 시간", example = "2025-10-29T10:00:00")
     LocalDateTime issuedAt,

    @Schema(description = "사용 시간 (사용한 경우만)", example = "2025-10-29T09:00:00")
     LocalDateTime usedAt,

    @Schema(description = "만료 시간", example = "2025-11-29T23:59:59")
     LocalDateTime expiresAt
){}
