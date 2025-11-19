package com.hhplus.hhplus_ecommerce.point.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "포인트 거래 내역")
public record TransactionDto (

    @Schema(description = "거래 ID", example = "1")
    Long id,

    @Schema(description = "거래 타입 (CHARGE: 충전, USE: 사용)", example = "CHARGE")
    String type,

    @Schema(description = "거래 금액", example = "100000")
    Long amount,

    @Schema(description = "거래 후 포인트", example = "150000")
    Long balanceAfter,

    @Schema(description = "거래 시간", example = "2025-10-29T10:00:00")
    LocalDateTime createdAt
){}