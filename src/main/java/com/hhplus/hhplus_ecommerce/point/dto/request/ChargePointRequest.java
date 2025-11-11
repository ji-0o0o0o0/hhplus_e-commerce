package com.hhplus.hhplus_ecommerce.point.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "포인트 충전 요청")
public class ChargePointRequest {

    @NotNull(message = "사용자 ID는 필수입니다")
    @Schema(description = "사용자 ID", example = "1", required = true)
    private Long userId;

    @NotNull(message = "충전 금액은 필수입니다")
    @Min(value = 1, message = "충전 금액은 0보다 커야 합니다")
    @Schema(description = "충전할 포인트", example = "100000", required = true)
    private Integer amount;
}