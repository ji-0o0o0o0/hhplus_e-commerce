package com.hhplus.hhplus_ecommerce.payment.domain;

import com.hhplus.hhplus_ecommerce.payment.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private Long id;
    private Long orderId;
    private Long userId;
    private Integer pointAmount;
    private Integer finalAmount;
    private PaymentStatus status;
    private LocalDateTime paymentDate;

    // 결제 생성
    public static Payment create( Long orderId, Long userId,
                                 Integer pointAmount, Integer finalAmount) {
        return Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .pointAmount(pointAmount)
                .finalAmount(finalAmount)
                .status(PaymentStatus.PENDING)
                .paymentDate(LocalDateTime.now())
                .build();
    }

    // 비즈니스 로직: 결제 완료
    public void complete() {
        this.status = PaymentStatus.COMPLETED;
        this.paymentDate = LocalDateTime.now();
    }

    // 비즈니스 로직: 결제 실패
    public void fail() {
        this.status = PaymentStatus.FAILED;
    }

    // 비즈니스 로직: 결제 취소
    public void cancel() {
        this.status = PaymentStatus.CANCELED;
    }
}