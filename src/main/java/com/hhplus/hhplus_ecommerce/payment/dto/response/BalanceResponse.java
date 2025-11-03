package com.hhplus.hhplus_ecommerce.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "잔액 응답")
public class BalanceResponse {

    @Schema(description = "잔액 ID", example = "1")
    private Long balanceId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "잔액", example = "100000")
    private Integer amount;

    @Schema(description = "마지막 업데이트 시간", example = "2025-10-29T10:00:00")
    private LocalDateTime updatedAt;
}