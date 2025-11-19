package com.hhplus.hhplus_ecommerce.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Schema(description = "주문 목록 조회 응답")
public record OrderListResponse (

    @Schema(description = "주문 목록")
     List<OrderListDto> orders,

    @Schema(description = "전체 주문 수", example = "10")
     Long totalElements,

    @Schema(description = "전체 페이지 수", example = "1")
     Integer totalPages,

    @Schema(description = "현재 페이지", example = "0")
     Integer currentPage,

    @Schema(description = "페이지 크기", example = "10")
     Integer size
){}