package com.hhplus.hhplus_ecommerce.payment.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.payment.dto.request.ChargeBalanceRequest;
import com.hhplus.hhplus_ecommerce.payment.dto.response.BalanceResponse;
import com.hhplus.hhplus_ecommerce.payment.dto.response.TransactionDto;
import com.hhplus.hhplus_ecommerce.payment.dto.response.TransactionListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/balances")
@Tag(name = "잔액 API", description = "잔액 조회 및 충전 API")
public class BalanceController {

    @GetMapping
    @Operation(summary = "잔액 조회", description = "사용자 잔액을 조회 합니다. ")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @RequestParam Long userId
    ) {
        BalanceResponse response = new BalanceResponse(
                1L,
                userId,
                100000,
                LocalDateTime.now()
        );
        return ResponseEntity.ok(ApiResponse.success(response));

    }
    @PostMapping("/charge")
    @Operation(summary = "잔액 충전", description = "사용자의 포인트를 충전합니다. ")
    public ResponseEntity<ApiResponse<BalanceResponse>> chargeBalance(
            @Valid @RequestBody ChargeBalanceRequest  request
            ) {
        BalanceResponse response = new BalanceResponse(
                1L,
                request.getUserId(),
                request.getAmount(),
                LocalDateTime.now()
        );
        return ResponseEntity.ok(ApiResponse.success(response));

    }
    @GetMapping("/transactions")
    @Operation(summary = "거래 내역 조회", description = "사용자 point 거래 내역을 조회 합니다. ")
    public ResponseEntity<ApiResponse<TransactionListResponse>> getTransactionsBalance(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @RequestParam Long userId,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "거래타입", example = "CHARGE")
            @RequestParam(defaultValue = "CHARGE") String type
    ) {
        List<TransactionDto> transactionBalace = List.of(
                new TransactionDto(1L,"CHARGE",100000,150000, LocalDateTime.now().minusDays(1)),
                new TransactionDto(1L,"USE",50000,100000, LocalDateTime.now())

        );

        TransactionListResponse response = new TransactionListResponse(
                transactionBalace,
                20L,
                2,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(response));

    }
}
