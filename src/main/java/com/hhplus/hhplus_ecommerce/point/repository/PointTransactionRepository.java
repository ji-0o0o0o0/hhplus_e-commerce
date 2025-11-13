package com.hhplus.hhplus_ecommerce.point.repository;

import com.hhplus.hhplus_ecommerce.point.domain.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction,Long> {
    List<PointTransaction> findByPointId(Long pointId);
    @Query("SELECT pt FROM PointTransaction pt WHERE pt.pointId = :pointId ORDER BY pt.createdAt DESC")
    List<PointTransaction> findByPointIdOrderByCreatedAtDesc(
            @Param("pointId") Long pointId,
            Pageable pageable
    );
}