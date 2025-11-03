package com.hhplus.hhplus_ecommerce.coupon.dto.response;

import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "사용자 쿠폰 정보")
public class UserCouponDto {

    @Schema(description = "사용자 쿠폰 ID", example = "1")
    private Long userCouponId;

    @Schema(description = "쿠폰 ID", example = "1")
    private Long couponId;

    @Schema(description = "쿠폰명", example = "20% 할인 쿠폰")
    private String couponName;

    @Schema(description = "할인율 (%)", example = "20")
    private Integer discountRate;

    @Schema(description = "쿠폰 상태 (AVAILABLE, USED, EXPIRED)", example = "AVAILABLE")
    private CouponStatus status;

    @Schema(description = "발급 시간", example = "2025-10-29T10:00:00")
    private LocalDateTime issuedAt;

    @Schema(description = "사용 시간 (사용한 경우만)", example = "2025-10-29T09:00:00")
    private LocalDateTime usedAt;

    @Schema(description = "만료 시간", example = "2025-11-29T23:59:59")
    private LocalDateTime expiresAt;
}
