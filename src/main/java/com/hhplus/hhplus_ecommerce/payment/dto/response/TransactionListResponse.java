package com.hhplus.hhplus_ecommerce.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "거래 내역 조회 응답")
public class TransactionListResponse {

    @Schema(description = "거래 내역 목록")
    private List<TransactionDto> transactions;

    @Schema(description = "전체 거래 수", example = "20")
    private Long totalElements;

    @Schema(description = "전체 페이지 수", example = "2")
    private Integer totalPages;

    @Schema(description = "현재 페이지", example = "0")
    private Integer currentPage;

    @Schema(description = "페이지 크기", example = "10")
    private Integer size;
}