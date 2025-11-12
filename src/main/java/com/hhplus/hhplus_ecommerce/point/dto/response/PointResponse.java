package com.hhplus.hhplus_ecommerce.point.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "포인트 응답")
public record PointResponse (

    @Schema(description = "포인트 ID", example = "1")
    Long id,

    @Schema(description = "사용자 ID", example = "1")
    Long userId,

    @Schema(description = "포인트 금액", example = "100000")
    Integer amount,

    @Schema(description = "마지막 업데이트 시간", example = "2025-10-29T10:00:00")
    LocalDateTime updatedAt
){}