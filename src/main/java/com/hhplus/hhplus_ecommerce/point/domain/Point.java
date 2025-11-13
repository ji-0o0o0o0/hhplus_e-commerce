package com.hhplus.hhplus_ecommerce.point.domain;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "points")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Point {

    private static final long MAX_CHARGE_AMOUNT_ONCE = 1_000_000L; // 1회 최대 충전 금액: 100만원
    private static final long MAX_POINT_BALANCE = 10_000_000L;     // 최대 보유 포인트: 1000만원

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true)
    private Long userId;

    @Column(nullable = false)
    private Long amount;

    @Version
    private Long version;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;


    // 새로운 포인트 생성 (초기 잔액 0)
    public static Point create(Long userId) {
        return Point.builder()
                .userId(userId)
                .amount(0L)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // 비즈니스 로직
    public void charge(Long chargeAmount) {
        if (chargeAmount <= 0) {
            throw new BusinessException(ErrorCode.POINT_INVALID_CHARGE_AMOUNT);
        }
        if (chargeAmount > MAX_CHARGE_AMOUNT_ONCE) {
            throw new BusinessException(ErrorCode.POINT_CHARGE_AMOUNT_EXCEEDS_ONCE);
        }
        if (this.amount + chargeAmount > MAX_POINT_BALANCE) {
            throw new BusinessException(ErrorCode.POINT_MAX_BALANCE_EXCEEDED);
        }
        this.amount += chargeAmount;
        this.updatedAt = LocalDateTime.now();
    }

    public void use(Long useAmount) {
        if (useAmount <= 0) {
            throw new BusinessException(ErrorCode.POINT_INVALID_USE_AMOUNT);
        }
        if (this.amount < useAmount) {
            throw new BusinessException(ErrorCode.POINT_INSUFFICIENT_BALANCE);
        }
        this.amount -= useAmount;
        this.updatedAt = LocalDateTime.now();
    }
    public boolean hasSufficientBalance(Long amount) {
        return this.amount >= amount;
    }
}