package com.hhplus.hhplus_ecommerce.point.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.point.TransactionType;
import com.hhplus.hhplus_ecommerce.point.domain.Point;
import com.hhplus.hhplus_ecommerce.point.domain.PointTransaction;
import com.hhplus.hhplus_ecommerce.point.repository.PointRepository;
import com.hhplus.hhplus_ecommerce.point.repository.PointTransactionRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {
    private final PointRepository pointRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PointTransactionService pointTransactionService;

    public Point getPoint(Long userId) {
        return pointRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Point newPoint = Point.create(userId);
                    return pointRepository.save(newPoint);
                });
    }

    @Retryable(
            retryFor = {ObjectOptimisticLockingFailureException.class, OptimisticLockException.class},
            maxAttempts = 10,
            backoff = @Backoff(delay = 50, maxDelay = 200, random = true)
    )
    public Point changePoint(Long userId, Long amount) {
        return pointTransactionService.executeChargeWithTransaction(userId, amount);
    }
    public Point usePoint(Long userId, Long amount) {
        Point point = getPoint(userId);
        point.use(amount);
        Point savedPoint = pointRepository.save(point);
        PointTransaction transaction = PointTransaction.create(savedPoint.getId(),amount, TransactionType.USE,savedPoint.getAmount());
        pointTransactionRepository.save(transaction);

        return  savedPoint;
    }

    public List<PointTransaction> getPointTransactions(Long userId) {
        Point point = pointRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POINT_NOT_FOUND));

        return pointTransactionRepository.findByPointId(point.getId());
    }
}
