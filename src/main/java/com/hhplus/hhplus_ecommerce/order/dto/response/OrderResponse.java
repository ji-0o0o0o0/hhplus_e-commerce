package com.hhplus.hhplus_ecommerce.order.dto.response;

import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "주문 응답")
public class OrderResponse {

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "총 금액", example = "55000")
    private Integer totalAmount;

    @Schema(description = "할인 금액", example = "5000")
    private Integer discountAmount;

    @Schema(description = "최종 금액", example = "50000")
    private Integer finalAmount;

    @Schema(description = "주문 상태 (PENDING, COMPLETED, CANCELLED)", example = "PENDING")
    private OrderStatus status;

    @Schema(description = "주문 항목 목록")
    private List<OrderItemDto> orderItems;

    @Schema(description = "주문 생성 시간", example = "2025-10-29T10:00:00")
    private LocalDateTime createdAt;
}