package com.hhplus.hhplus_ecommerce.order.repository;

import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.order.domain.Order;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByStatusAndCreatedAtBetween(
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * 특정 상태이면서 생성 시간이 특정 시점 이전인 주문 조회
     * - 결제 타임아웃 처리 시 사용
     */
    List<Order> findByStatusAndCreatedAtBefore(
            OrderStatus status,
            LocalDateTime timeoutThreshold
    );

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Order o set o.createdAt = :createdAt where o.id = :id")
    void updateCreatedAt(@Param("id") Long id,
                         @Param("createdAt") LocalDateTime createdAt);


    List<Order> findByIdInAndStatus(List<Long> ids, OrderStatus status);

}