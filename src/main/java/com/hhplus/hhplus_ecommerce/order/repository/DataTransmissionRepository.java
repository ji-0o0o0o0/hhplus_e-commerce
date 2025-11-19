package com.hhplus.hhplus_ecommerce.order.repository;

import com.hhplus.hhplus_ecommerce.order.TransmissionStatus;
import com.hhplus.hhplus_ecommerce.order.domain.DataTransmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataTransmissionRepository extends JpaRepository<DataTransmission, Long> {

    // 상태별 조회 (스케줄러에서 PENDING 조회)
    List<DataTransmission> findByStatus(TransmissionStatus status);

    // 재시도 횟수 제한 조회 (최대 재시도 횟수 미만)
    List<DataTransmission> findByStatusAndAttemptsLessThan(TransmissionStatus status, Integer maxAttempts);

    List<DataTransmission> findByOrderId(Long orderId);
}
