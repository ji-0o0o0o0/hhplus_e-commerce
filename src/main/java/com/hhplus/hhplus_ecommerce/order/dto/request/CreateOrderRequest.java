package com.hhplus.hhplus_ecommerce.order.dto.request;

import com.hhplus.hhplus_ecommerce.order.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "주문 생성 요청")
public class
CreateOrderRequest {

    @NotNull(message = "사용자 ID는 필수입니다")
    @Schema(description = "사용자 ID", example = "1", required = true)
    private Long userId;

    @NotNull(message = "주문 타입은 필수입니다")
    @Schema(description = "주문 타입 (CART: 장바구니, DIRECT: 즉시구매)", example = "CART", required = true)
    private OrderType orderType;

    @Schema(description = "쿠폰 ID (선택)", example = "1")
    private Long couponId;

    @Schema(description = "즉시 구매 시 상품 목록 (orderType=DIRECT 시 필수)")
    private List<OrderItemRequest> items;

    @Getter
    @NoArgsConstructor
    @Schema(description = "주문 상품 정보")
    public static class OrderItemRequest {

        @NotNull(message = "상품 ID는 필수입니다")
        @Schema(description = "상품 ID", example = "1", required = true)
        private Long productId;

        @NotNull(message = "수량은 필수입니다")
        @Schema(description = "수량", example = "2", required = true)
        private Integer quantity;
    }
}