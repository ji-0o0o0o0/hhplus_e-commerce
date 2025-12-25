package com.hhplus.hhplus_ecommerce.common.kafka.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueRequestMessage {

    private String requestId;        // 멱등성 키 (UUID)
    private Long userId;             // 사용자 ID
    private Long couponId;           // 쿠폰 ID

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime requestedAt; // 요청 시간
}