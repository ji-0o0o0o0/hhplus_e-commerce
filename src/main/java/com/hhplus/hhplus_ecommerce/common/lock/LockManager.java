package com.hhplus.hhplus_ecommerce.common.lock;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
public class LockManager {

    private final Map<String, Lock> locks = new ConcurrentHashMap<>();
    private static final long LOCK_TIMEOUT_SECONDS = 10;

    public <T> T executeWithLock(String key, Supplier<T> action) {
        Lock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());

        try {
            if (!lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }
            try {
                return action.get();
            } finally {
                lock.unlock();
            }
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.LOCK_INTERRUPTED);
        }
    }

    public void executeWithLock(String key, Runnable action) {
        Lock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        try {
            if (!lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }
            try {
                action.run();
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.LOCK_INTERRUPTED);
        }
    }

    //테스트용: 모든 Lock 제거
    public void clearAllLocks() {
        locks.clear();
    }

    //현재 관리 중인 Lock 개수 조회 (모니터링용)
    public int getLockCount() {
        return locks.size();
    }
}
