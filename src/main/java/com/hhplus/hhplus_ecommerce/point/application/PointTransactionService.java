package com.hhplus.hhplus_ecommerce.point.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.point.TransactionType;
import com.hhplus.hhplus_ecommerce.point.domain.Point;
import com.hhplus.hhplus_ecommerce.point.domain.PointTransaction;
import com.hhplus.hhplus_ecommerce.point.repository.PointRepository;
import com.hhplus.hhplus_ecommerce.point.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointTransactionService {
    private final PointRepository pointRepository;
    private final PointTransactionRepository pointTransactionRepository;

    @Transactional
    public Point executeChargeWithTransaction(Long userId, Long amount) {
        Point point = pointRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Point newPoint = Point.create(userId);
                    return pointRepository.save(newPoint);
                });

        point.charge(amount);
        Point savedPoint = pointRepository.save(point);

        PointTransaction transaction = PointTransaction.create(
                savedPoint.getId(),
                amount,
                TransactionType.CHARGE,
                savedPoint.getAmount()
        );
        pointTransactionRepository.save(transaction);

        return savedPoint;
    }

    @Transactional
    public Point chargePointTransaction(Long userId, Long amount) {
        Point point = pointRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Point newPoint = Point.create(userId);
                    return pointRepository.save(newPoint);
                });

        point.charge(amount);
        Point savedPoint = pointRepository.save(point);

        PointTransaction transaction = PointTransaction.create(
                savedPoint.getId(),
                amount,
                TransactionType.CHARGE,
                savedPoint.getAmount()
        );
        pointTransactionRepository.save(transaction);

        return savedPoint;
    }
    @Transactional
    public Point usePointTransaction(Long userId, Long amount) {
        Point point = pointRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POINT_NOT_FOUND));

        point.use(amount);
        Point savedPoint = pointRepository.save(point);

        PointTransaction transaction = PointTransaction.create(
                savedPoint.getId(),
                amount,
                TransactionType.USE,
                savedPoint.getAmount()
        );
        pointTransactionRepository.save(transaction);

        return savedPoint;
    }

}
