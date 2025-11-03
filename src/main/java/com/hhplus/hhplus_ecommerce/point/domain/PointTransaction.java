package com.hhplus.hhplus_ecommerce.point.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTransaction {

    private Long id;
    private Long userId;
    private TransactionType type;
    private Integer amount;
    private Integer balanceAfter;
    private LocalDateTime createdAt;



    public static PointTransaction createCharge(Long userId, Integer amount, Integer balanceAfter) {
        return PointTransaction.builder()
                .userId(userId)
                .type(TransactionType.CHARGE)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .createdAt(LocalDateTime.now())
                .build();
    }
    public static PointTransaction createUse(Long userId, Integer amount, Integer balanceAfter) {
        return PointTransaction.builder()
                .userId(userId)
                .type(TransactionType.USE)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public enum TransactionType {
        CHARGE("충전"),
        USE("사용");

        private final String description;

        TransactionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}