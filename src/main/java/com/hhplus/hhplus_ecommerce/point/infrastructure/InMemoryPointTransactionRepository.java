package com.hhplus.hhplus_ecommerce.point.infrastructure;

import com.hhplus.hhplus_ecommerce.point.domain.PointTransaction;
import com.hhplus.hhplus_ecommerce.point.repository.PointTransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryPointTransactionRepository implements PointTransactionRepository {

    private final Map<Long, PointTransaction> store = new ConcurrentHashMap<>();
    private final Map<Long, List<PointTransaction>> userIdIndex = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public PointTransaction save(PointTransaction transaction) {
        if (transaction.getId() == null) {
            PointTransaction newTransaction = PointTransaction.builder()
                    .id(idGenerator.getAndIncrement())
                    .pointId(transaction.getPointId())
                    .type(transaction.getType())
                    .amount(transaction.getAmount())
                    .balanceAfter(transaction.getBalanceAfter())
                    .createdAt(transaction.getCreatedAt())
                    .build();
            store.put(newTransaction.getId(), newTransaction);

            // userId 인덱스 업데이트
            userIdIndex.computeIfAbsent(newTransaction.getPointId(), k -> new ArrayList<>())
                    .add(newTransaction);

            return newTransaction;
        } else {
            store.put(transaction.getId(), transaction);
            return transaction;
        }
    }

    @Override
    public List<PointTransaction> findByPointId(Long pointId) {
        return userIdIndex.getOrDefault(pointId, new ArrayList<>()).stream()
                .sorted(Comparator.comparing(PointTransaction::getCreatedAt).reversed())
                .toList();
    }

    @Override
    public List<PointTransaction> findByPointId(Long pointId, int offset, int limit) {
        return userIdIndex.getOrDefault(pointId, new ArrayList<>()).stream()
                .sorted(Comparator.comparing(PointTransaction::getCreatedAt).reversed())
                .skip(offset)
                .limit(limit)
                .toList();
    }


    public void clear() {
        store.clear();
        userIdIndex.clear();
        idGenerator.set(1);
    }
}