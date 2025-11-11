package com.hhplus.hhplus_ecommerce.point.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.point.dto.request.ChargePointRequest;
import com.hhplus.hhplus_ecommerce.point.dto.response.PointResponse;
import com.hhplus.hhplus_ecommerce.point.dto.response.TransactionListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 포인트 API 명세
 * - Swagger 문서화를 위한 인터페이스
 * - API 명세와 실제 구현을 분리
 */
@Tag(name = "포인트 API", description = "포인트 조회 및 충전 API")
public interface PointApi {

    /**
     * 포인트 잔액 조회
     */
    @GetMapping
    @Operation(summary = "포인트 잔액 조회", description = "사용자의 포인트 잔액을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": true,
                                  "data": {
                                    "id": 1,
                                    "userId": 1,
                                    "amount": 100000,
                                    "updatedAt": "2024-11-03T18:00:00"
                                  }
                                }
                                """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": false,
                                  "code": 404,
                                  "message": "사용자를 찾을 수 없습니다."
                                }
                                """))
            )
    })
    ResponseEntity<ApiResponse<PointResponse>> getBalance(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @RequestParam Long userId
    );

    /**
     * 포인트 충전
     */
    @PostMapping("/charge")
    @Operation(summary = "포인트 충전", description = "사용자의 포인트를 충전합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "충전 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": true,
                                  "data": {
                                    "id": 1,
                                    "userId": 1,
                                    "amount": 150000,
                                    "updatedAt": "2024-11-03T18:00:00"
                                  }
                                }
                                """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": false,
                                  "code": 400,
                                  "message": "충전 금액은 0보다 커야 합니다."
                                }
                                """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": false,
                                  "code": 404,
                                  "message": "사용자를 찾을 수 없습니다."
                                }
                                """))
            )
    })
    ResponseEntity<ApiResponse<PointResponse>> chargePoint(
            @Valid @RequestBody ChargePointRequest request
    );

    /**
     * 포인트 거래 내역 조회
     */
    @GetMapping("/transactions")
    @Operation(summary = "포인트 거래 내역 조회", description = "사용자의 포인트 거래 내역을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": true,
                                  "data": {
                                    "transactions": [
                                      {
                                        "userId": 1,
                                        "type": "CHARGE",
                                        "amount": 100000,
                                        "balanceAfter": 150000,
                                        "createdAt": "2024-11-02T18:00:00"
                                      }
                                    ],
                                    "totalElements": 20,
                                    "totalPages": 2,
                                    "currentPage": 0,
                                    "pageSize": 10
                                  }
                                }
                                """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "success": false,
                                  "code": 404,
                                  "message": "사용자를 찾을 수 없습니다."
                                }
                                """))
            )
    })
    ResponseEntity<ApiResponse<TransactionListResponse>> getTransactions(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @RequestParam Long userId,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") Integer size,

            @Parameter(description = "거래타입 (CHARGE: 충전, USE: 사용)", example = "CHARGE")
            @RequestParam(defaultValue = "CHARGE") String type
    );
}