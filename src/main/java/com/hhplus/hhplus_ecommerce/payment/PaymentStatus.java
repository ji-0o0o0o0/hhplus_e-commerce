package com.hhplus.hhplus_ecommerce.payment;

public enum PaymentStatus {
    PENDING,        // 결제 대기
    PROCESSING,     // 결제 처리 중
    COMPLETED,      // 결제 완료
    FAILED,         // 결제 실패
    CANCELED        // 결제 취소
}
