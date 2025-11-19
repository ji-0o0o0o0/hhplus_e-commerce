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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                    // Point 없으면 0원으로 생성
                    Point newPoint = Point.create(userId);
                    return pointRepository.save(newPoint);
                });
    }
    public Point changePoint(Long userId, Long amount) {
        int maxRetries = 100;  // 재시도 횟수 5->100
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                return pointTransactionService.executeChargeWithTransaction(userId, amount);
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw new BusinessException(ErrorCode.CONCURRENCY_CONFLICT);
                }
                // 재시도 전 랜덤 대기 (1~10ms)
                try {
                    Thread.sleep((long) (Math.random() * 10 + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(ErrorCode.CONCURRENCY_CONFLICT);
                }
            }
        }

        throw new BusinessException(ErrorCode.CONCURRENCY_CONFLICT);
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
