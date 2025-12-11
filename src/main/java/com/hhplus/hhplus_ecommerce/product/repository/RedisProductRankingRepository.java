package com.hhplus.hhplus_ecommerce.product.repository;

import com.hhplus.hhplus_ecommerce.product.domain.ProductStatistics;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RedisProductRankingRepository implements ProductRankingRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String RANKING_KEY_PREFIX = "product:ranking:";


    @Override
    public void incrementSales(Long productId, int quantity) {
        String member = createMember(productId);
        String todayKey = getTodayRankingKey();
        redisTemplate.opsForZSet().incrementScore(todayKey, member, quantity);
    }

    @Override
    public List<Long> getTopProductIds(int limit) {

        List<String> keys = getRecentRankingKeys(3);

        //임시 키로 병합 (ZUNIONSTORE)
        String tempKey ="product:ranking:temp:"+ UUID.randomUUID();
        redisTemplate.opsForZSet().unionAndStore(keys.get(0),List.of(keys.get(1),keys.get(2)),tempKey);

        //TOP N 조회
        Set<String> topMembers = redisTemplate.opsForZSet()
                .reverseRange(tempKey, 0, limit - 1);

        //임시 키 삭제
        redisTemplate.delete(tempKey);

        if (topMembers == null || topMembers.isEmpty()) {
            return List.of();
        }

        return topMembers.stream()
                .map(this::extractProductId)
                .collect(Collectors.toList());
    }

        @Override
    public Long getSalesCount(Long productId) {
        String member = createMember(productId);
        List<String> keys = getRecentRankingKeys(3);
        long totalSales = 0;
        for (String key : keys) {
            Double score = redisTemplate.opsForZSet().score(key, member);
            totalSales += (score != null ? score.longValue() : 0L);
        }

        return totalSales;
    }

    @Override
    public Long getRank(Long productId) {
        String member = createMember(productId);
        // 임시 키로 3일 병합
        List<String> keys = getRecentRankingKeys(3);
        String tempKey = "product:ranking:temp:" + UUID.randomUUID();

        if (keys.size() >= 2) {
            redisTemplate.opsForZSet().unionAndStore(
                    keys.get(0),
                    keys.subList(1, keys.size()),
                    tempKey
            );
        }

        Long rank = redisTemplate.opsForZSet().reverseRank(tempKey, member);
        redisTemplate.delete(tempKey);

        return rank != null ? rank + 1 : null;

    }

    @Override
    public Map<Long, Long> getSalesCountBulk(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new HashMap<>();
        }
        List<String> keys = getRecentRankingKeys(3);
        Map<Long, Long> result = new HashMap<>();

        for (Long productId : productIds) {
            String member = createMember(productId);
            long totalSales = 0;

            // 3일치 점수 조회 (Pipeline 사용)
            List<Object> scores = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (String key : keys) {
                    connection.zSetCommands().zScore(
                            key.getBytes(),
                            member.getBytes()
                    );
                }
                return null;
            });

            for (Object scoreObj : scores) {
                Double score = (Double) scoreObj;
                totalSales += (score != null ? score.longValue() : 0L);
            }

            result.put(productId, totalSales);
        }


        return result;
    }

    private String getTodayRankingKey() {
        return getRankingKey(LocalDate.now());
    }

    private String getRankingKey(LocalDate date) {
        return RANKING_KEY_PREFIX + date;
    }

    private List<String> getRecentRankingKeys(int days) {
        LocalDate today = LocalDate.now();
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            keys.add(getRankingKey(today.minusDays(i)));
        }
        return keys;
    }

    //DB 기반 복구 메서드
    public void rebuildFromStatistics(List<ProductStatistics> statistics) {
        statistics.forEach(stat -> {
            String key = getRankingKey(stat.getStatsDate());
            String member = createMember(stat.getProductId());
            redisTemplate.opsForZSet().add(key, member, stat.getSalesCount());
        });
    }


    @Override
    public Map<Long, Long> getRankBulk(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new HashMap<>();
        }
        List<String> keys = getRecentRankingKeys(3);
        String tempKey = "product:ranking:temp:" + UUID.randomUUID();

        try {
            // ZUNIONSTORE로 병합
            if (keys.size() >= 2) {
                redisTemplate.opsForZSet().unionAndStore(
                        keys.get(0),
                        keys.subList(1, keys.size()),
                        tempKey
                );
            } else if (keys.size() == 1) {
                // 키가 1개면 복사
                redisTemplate.opsForZSet().unionAndStore(keys.get(0), List.of(), tempKey);
            }

            // Pipeline으로 한 번에 순위 조회
            List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (Long productId : productIds) {
                    String member = createMember(productId);
                    connection.zSetCommands().zRevRank(
                            tempKey.getBytes(),
                            member.getBytes()
                    );
                }
                return null;
            });

            // 결과 매핑
            Map<Long, Long> result = new HashMap<>();
            for (int i = 0; i < productIds.size(); i++) {
                Long rank = (Long) results.get(i);
                result.put(productIds.get(i), rank != null ? rank + 1 : null);
            }

            return result;

        } finally {
            // 임시 키 삭제 (반드시 실행)
            redisTemplate.delete(tempKey);
        }

    }

    private String createMember(Long productId) {
        return "product:" + productId;
    }

    private Long extractProductId(String member) {
        return Long.parseLong(member.substring(8));
    }
}