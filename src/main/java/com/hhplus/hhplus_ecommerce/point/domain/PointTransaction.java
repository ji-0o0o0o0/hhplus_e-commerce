package com.hhplus.hhplus_ecommerce.point.domain;

import com.hhplus.hhplus_ecommerce.point.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "point_transactions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pointId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;



    public static PointTransaction create(Long pointId, Long amount,TransactionType type, Long balanceAfter) {
        return PointTransaction.builder()
                .pointId(pointId)
                .type(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .build();
    }
}