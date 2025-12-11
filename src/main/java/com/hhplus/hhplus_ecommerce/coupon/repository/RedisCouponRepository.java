package com.hhplus.hhplus_ecommerce.coupon.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis 기반 쿠폰 발급 저장소
 * - 재고 관리 (String with INCR/DECR)
 * - 중복 발급 방지 (Set)
 * - 비동기 대기열 (List)
 */
@Repository
@RequiredArgsConstructor
public class RedisCouponRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String COUPON_ISSUED_PREFIX = "coupon:issued:";
    private static final String COUPON_QUEUE_PREFIX = "coupon:queue:";
    private static final String COUPON_VALID_PREFIX = "coupon:valid:";
    

    public Boolean addIssuedUser(Long couponId, Long userId) {
        String key = COUPON_ISSUED_PREFIX + couponId;
        return redisTemplate.opsForSet().add(key, userId.toString()) > 0;
    }

    public Boolean removeIssuedUser(Long couponId, Long userId) {
        String key = COUPON_ISSUED_PREFIX + couponId;
        return redisTemplate.opsForSet().remove(key, userId.toString()) > 0;
    }

    public Boolean isAlreadyIssued(Long couponId, Long userId) {
        String key = COUPON_ISSUED_PREFIX + couponId;
        return redisTemplate.opsForSet().isMember(key, userId.toString());
    }

    public Long getIssuedCount(Long couponId) {
        String key = COUPON_ISSUED_PREFIX + couponId;
        return redisTemplate.opsForSet().size(key);
    }

    public void clear(Long couponId) {
        redisTemplate.delete(COUPON_ISSUED_PREFIX + couponId);
        redisTemplate.delete(COUPON_QUEUE_PREFIX + couponId);
    }

    // ===== 비동기 대기열 관련 메서드 =====

    //대기열에 쿠폰 발급 요청 추가
    public void addToQueue(Long couponId, CouponIssueRequest request) {
        String key = COUPON_QUEUE_PREFIX + couponId;
        try {
            String json = objectMapper.writeValueAsString(request);
            redisTemplate.opsForList().rightPush(key, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize coupon issue request", e);
        }
    }

    //대기열에서 쿠폰 발급 요청을 Bulk로 꺼내기
    public List<CouponIssueRequest> popFromQueue(Long couponId, int count) {
        String key = COUPON_QUEUE_PREFIX + couponId;
        List<CouponIssueRequest> requests = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String json = redisTemplate.opsForList().leftPop(key);
            if (json == null) {
                break;
            }
            try {
                CouponIssueRequest request = objectMapper.readValue(json, CouponIssueRequest.class);
                requests.add(request);
            } catch (JsonProcessingException e) {
                // 파싱 실패 시 로깅하고 스킵
                System.err.println("Failed to deserialize coupon issue request: " + json);
            }
        }

        return requests;
    }

    //대기열 크기 조회
    public Long getQueueSize(Long couponId) {
        String key = COUPON_QUEUE_PREFIX + couponId;
        return redisTemplate.opsForList().size(key);
    }

    //대기열이 있는 쿠폰 ID 목록 조회 (스케줄러용)
    public List<Long> getCouponIdsWithQueue() {
        var keys = redisTemplate.keys(COUPON_QUEUE_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        return keys.stream()
                .map(key -> {
                    String couponIdStr = key.replace(COUPON_QUEUE_PREFIX, "");
                    return Long.parseLong(couponIdStr);
                })
                .filter(couponId -> {
                    Long size = getQueueSize(couponId);
                    return size != null && size > 0;
                })
                .toList();
    }

    public void setExpire(Long couponId, Duration ttl) {
        String issuedKey = COUPON_ISSUED_PREFIX + couponId;
        String queueKey = COUPON_QUEUE_PREFIX + couponId;
        String validKey = COUPON_VALID_PREFIX + couponId;

        redisTemplate.expire(issuedKey, ttl);
        redisTemplate.expire(queueKey, ttl);
        redisTemplate.expire(validKey, ttl);

    }
    public void setCouponValidity(Long couponId, LocalDateTime startDate, LocalDateTime endDate, Integer totalQuantity,Duration ttl) {
        String key = COUPON_VALID_PREFIX + couponId;
        redisTemplate.opsForHash().put(key,"startDate",startDate.toString());
        redisTemplate.opsForHash().put(key,"endDate",endDate.toString());
        redisTemplate.opsForHash().put(key,"totalQuantity",totalQuantity.toString());
        redisTemplate.expire(key,ttl);
    }
    //쿠폰 유효성 검사(Redis)
    public boolean isCouponValid(Long couponId) {
        String key = COUPON_VALID_PREFIX + couponId;
        String startDateStr = (String) redisTemplate.opsForHash().get(key,"startDate");
        String endDateStr = (String) redisTemplate.opsForHash().get(key,"endDate");

        if (startDateStr == null || endDateStr == null) {
            return false;
        }

        LocalDateTime startDate = LocalDateTime.parse(startDateStr);
        LocalDateTime endDate = LocalDateTime.parse(endDateStr);
        LocalDateTime now = LocalDateTime.now();

        return !now.isBefore(startDate) && !now.isAfter(endDate);
    }
    public Integer getTotalQuantity(Long couponId) {
        String key = COUPON_VALID_PREFIX + couponId;
        String totalQuantityStr = (String) redisTemplate.opsForHash().get(key, "totalQuantity");
        return totalQuantityStr != null ? Integer.parseInt(totalQuantityStr) : null;
    }


    public record CouponIssueRequest(Long userId, Long couponId,int retryCount) {
    }
}