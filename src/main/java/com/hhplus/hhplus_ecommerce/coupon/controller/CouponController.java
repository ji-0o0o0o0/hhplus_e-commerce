package com.hhplus.hhplus_ecommerce.coupon.controller;

import com.hhplus.hhplus_ecommerce.common.dto.ApiResponse;
import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import com.hhplus.hhplus_ecommerce.coupon.dto.request.IssueCouponRequest;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponIssueResponse;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponListResponse;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.UserCouponDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.hhplus.hhplus_ecommerce.coupon.docs.CouponSwaggerDocs.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@Tag(name = "쿠폰 API", description = "쿠폰 관리 API")
public class CouponController {
    @PostMapping("/issue")
    @Operation(summary = "쿠폰 발급", description = "사용자에게 쿠폰을 발급한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "쿠폰 발급 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "성공", value = ISSUE_COUPON_SUCCESS))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "쿠폰 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "쿠폰 없음", value = COUPON_NOT_FOUND))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "발급 불가",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "이미 발급받음", value = ALREADY_ISSUED),
                                    @ExampleObject(name = "쿠폰 소진", value = COUPON_SOLD_OUT)
                            })
            )
    })
    public ResponseEntity<ApiResponse<CouponIssueResponse>> issueCoupon(
            @Valid @RequestBody IssueCouponRequest request
    ) {
        // Mock: 특정 쿠폰 ID는 없는 것으로 처리
        if (request.getCouponId() == 999L) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "쿠폰을 찾을 수 없습니다."));
        }

        // Mock: 특정 쿠폰 ID는 소진된 것으로 처리
        if (request.getCouponId() == 888L) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(409, "쿠폰이 모두 소진되었습니다."));
        }

        // Mock: 특정 사용자 ID는 이미 발급받은 것으로 처리
        if (request.getUserId() == 777L) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(409, "이미 발급받은 쿠폰입니다."));
        }

        CouponIssueResponse response = new CouponIssueResponse(
                1L,
                request.getUserId(),
                request.getCouponId(),
                "20% 할인 쿠폰",
                20,
                CouponStatus.AVAILABLE
                , LocalDateTime.now(),
                LocalDateTime.now().plusDays(1)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("쿠폰이 성공적으로 발급되었습니다", response));
    }

    @GetMapping
    @Operation(summary = "쿠폰 조회", description = "본인에게 발급된 쿠폰을 확인할 수 있다.")
    public ResponseEntity<ApiResponse<CouponListResponse>> getCoupons(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @RequestParam Long userId,
            @Parameter(description = "쿠폰 상태", example = "AVAILABLE", required = false)
            @RequestParam(required = false) CouponStatus status
    ) {
        List<UserCouponDto> userCouponList = List.of(
                new UserCouponDto(1L,1L,"20% 할인 쿠폰",20, CouponStatus.AVAILABLE,LocalDateTime.now().minusDays(2),LocalDateTime.now(),LocalDateTime.now()),
                new UserCouponDto(2L,2L,"10% 할인 쿠폰",10,CouponStatus.USED,LocalDateTime.now().minusDays(3),LocalDateTime.now(),LocalDateTime.now().plusDays(2))
        );

        CouponListResponse  response = new CouponListResponse(
                userCouponList
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
