package com.hhplus.hhplus_ecommerce.common.lock;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
public class LockManager {

    private final Map<String, Lock> locks = new ConcurrentHashMap<>();

    public <T> T executeWithLock(String key, Supplier<T> action) {
        Lock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();

        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    public void executeWithLock(String key, Runnable action) {
        Lock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();

        try {
            action.run();
        } finally {
            lock.unlock();
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
