package com.hhplus.hhplus_ecommerce.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockManager {

    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "lock:";
    private static final long DEFAULT_WAIT_TIME = 5L;
    private static final long DEFAULT_LEASE_TIME = 10L;

    /**
     * 분산락을 이용한 작업 실행 (Redisson Pub/Sub 방식)
     *
     * @param lockKey 락 키
     * @param waitTime 락 획득 대기 시간 (초)
     * @param leaseTime 락 유지 시간 (초)
     * @param task 실행할 작업
     * @return 작업 결과
     * @throws InterruptedException 락 획득 대기 중 인터럽트 발생
     * @throws LockAcquisitionException 락 획득 실패
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, Supplier<T> task) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);

            if (!acquired) {
                log.warn("Failed to acquire lock: {}", fullLockKey);
                throw new LockAcquisitionException("락 획득에 실패했습니다: " + lockKey);
            }

            log.debug("Lock acquired: {}", fullLockKey);
            return task.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lock acquisition interrupted: {}", fullLockKey, e);
            throw new LockAcquisitionException("락 획득 중 인터럽트 발생: " + lockKey, e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released: {}", fullLockKey);
            }
        }
    }

    /**
     * 분산락을 이용한 작업 실행 (기본 시간 설정)
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> task) {
        return executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, task);
    }

    /**
     * 분산락을 이용한 void 작업 실행
     */
    public void executeWithLock(String lockKey, long waitTime, long leaseTime, Runnable task) {
        executeWithLock(lockKey, waitTime, leaseTime, () -> {
            task.run();
            return null;
        });
    }

    /**
     * 분산락을 이용한 void 작업 실행 (기본 시간 설정)
     */
    public void executeWithLock(String lockKey, Runnable task) {
        executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, task);
    }
}
