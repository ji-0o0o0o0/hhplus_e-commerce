package com.hhplus.hhplus_ecommerce.order.domain;

import com.hhplus.hhplus_ecommerce.order.TransmissionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_transmissions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataTransmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransmissionStatus status;

    @Column(nullable = false)
    private Integer attempts;

    @Column(columnDefinition = "TEXT")  // ⭐ 추가
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime sentAt;

    @Column
    private LocalDateTime lastErrorAt;

    // 실패 기록
    public void recordFailure(String errorMessage) {
        this.status = TransmissionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.lastErrorAt = LocalDateTime.now();
        this.attempts++;
    }

    // 성공 기록
    public void markSuccess() {
        this.status = TransmissionStatus.SUCCESS;
        this.sentAt = LocalDateTime.now();
        this.errorMessage = null;
    }
}