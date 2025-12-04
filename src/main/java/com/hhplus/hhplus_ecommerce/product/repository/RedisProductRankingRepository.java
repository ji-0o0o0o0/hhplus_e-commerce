package com.hhplus.hhplus_ecommerce.product.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RedisProductRankingRepository implements ProductRankingRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String RANKING_KEY = "product:ranking";

    @Override
    public void incrementSales(Long productId, int quantity) {
        String member = createMember(productId);
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY, member, quantity);
    }

    @Override
    public List<Long> getTopProductIds(int limit) {
        Set<String> topMembers = redisTemplate.opsForZSet()
                .reverseRange(RANKING_KEY, 0, limit - 1);

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
        Double score = redisTemplate.opsForZSet().score(RANKING_KEY, member);
        return score != null ? score.longValue() : 0L;
    }

    @Override
    public Long getRank(Long productId) {
        String member = createMember(productId);
        Long rank = redisTemplate.opsForZSet().reverseRank(RANKING_KEY, member);
        return rank != null ? rank + 1 : null;
    }

    @Override
    public Map<Long, Long> getSalesCountBulk(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new HashMap<>();
        }

        // Pipeline으로 한 번에 조회
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long productId : productIds) {
                String member = createMember(productId);
                connection.zSetCommands().zScore(
                        RANKING_KEY.getBytes(),
                        member.getBytes()
                );
            }
            return null;  // Pipeline은 null 반환 필수
        });

        // 결과 매핑
        Map<Long, Long> result = new HashMap<>();
        for (int i = 0; i < productIds.size(); i++) {
            Double score = (Double) results.get(i);
            result.put(productIds.get(i), score != null ? score.longValue() : 0L);
        }

        return result;
    }

    @Override
    public Map<Long, Long> getRankBulk(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new HashMap<>();
        }

        // Pipeline으로 한 번에 조회
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long productId : productIds) {
                String member = createMember(productId);
                connection.zSetCommands().zRevRank(
                        RANKING_KEY.getBytes(),
                        member.getBytes()
                );
            }
            return null;  // Pipeline은 null 반환 필수
        });

        // 결과 매핑
        Map<Long, Long> result = new HashMap<>();
        for (int i = 0; i < productIds.size(); i++) {
            Long rank = (Long) results.get(i);
            result.put(productIds.get(i), rank != null ? rank + 1 : null);
        }

        return result;
    }

    private String createMember(Long productId) {
        return "product:" + productId;
    }

    private Long extractProductId(String member) {
        return Long.parseLong(member.substring(8));
    }
}