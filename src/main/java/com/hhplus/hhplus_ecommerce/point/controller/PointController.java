package com.hhplus.hhplus_ecommerce.point.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.point.dto.request.ChargePointRequest;
import com.hhplus.hhplus_ecommerce.point.dto.response.PointResponse;
import com.hhplus.hhplus_ecommerce.point.dto.response.TransactionDto;
import com.hhplus.hhplus_ecommerce.point.dto.response.TransactionListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/points")
public class PointController implements PointApi {


    @Override
    public ResponseEntity<ApiResponse<PointResponse>> getBalance(Long userId) {
        PointResponse response = new PointResponse(
                1L,
                userId,
                100000L,
                LocalDateTime.now()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<PointResponse>> chargePoint(ChargePointRequest request) {
        PointResponse response = new PointResponse(
                1L,
                request.userId(),
                request.amount(),
                LocalDateTime.now()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<TransactionListResponse>> getTransactions(
            Long userId, Integer page, Integer size, String type) {
        List<TransactionDto> transactions = List.of(
                new TransactionDto(1L, "CHARGE", 100000L, 150000L, LocalDateTime.now().minusDays(1)),
                new TransactionDto(1L, "USE", 50000L, 100000L, LocalDateTime.now())
        );

        TransactionListResponse response = new TransactionListResponse(
                transactions,
                20L,
                2,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

