package com.hhplus.hhplus_ecommerce.point.domain;

import com.hhplus.hhplus_ecommerce.point.TransactionType;
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



    public static PointTransaction create(Long userId, Integer amount,TransactionType type, Integer balanceAfter) {
        return PointTransaction.builder()
                .userId(userId)
                .type(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .createdAt(LocalDateTime.now())
                .build();
    }
}