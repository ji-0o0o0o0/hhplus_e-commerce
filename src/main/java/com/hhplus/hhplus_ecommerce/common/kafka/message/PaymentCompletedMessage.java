package com.hhplus.hhplus_ecommerce.common.kafka.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedMessage {

    private Long orderId;
    private Long userId;
    private Long totalAmount;
    private Long discountAmount;
    private Long finalAmount;
    private Long couponId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;
}