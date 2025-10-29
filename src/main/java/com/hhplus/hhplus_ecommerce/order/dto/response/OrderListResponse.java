package com.hhplus.hhplus_ecommerce.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "주문 목록 조회 응답")
public class OrderListResponse {

    @Schema(description = "주문 목록")
    private List<OrderListDto> orders;

    @Schema(description = "전체 주문 수", example = "10")
    private Long totalElements;

    @Schema(description = "전체 페이지 수", example = "1")
    private Integer totalPages;

    @Schema(description = "현재 페이지", example = "0")
    private Integer currentPage;

    @Schema(description = "페이지 크기", example = "10")
    private Integer size;
}