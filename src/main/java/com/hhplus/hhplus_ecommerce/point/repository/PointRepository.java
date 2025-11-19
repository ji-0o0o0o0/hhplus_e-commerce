package com.hhplus.hhplus_ecommerce.point.repository;

import com.hhplus.hhplus_ecommerce.point.domain.Point;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointRepository extends JpaRepository<Point, Long> {

    Optional<Point> findByUserId(Long userId);
}