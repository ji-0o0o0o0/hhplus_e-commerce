package com.hhplus.hhplus_ecommerce.order.dto.response;

import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "주문 목록 항목")
public class OrderListDto {

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "최종 금액", example = "50000")
    private Integer finalAmount;

    @Schema(description = "주문 상태", example = "COMPLETED")
    private OrderStatus status;

    @Schema(description = "주문 생성 시간", example = "2025-10-29T10:00:00")
    private LocalDateTime createdAt;
}