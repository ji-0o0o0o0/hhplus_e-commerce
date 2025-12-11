package com.hhplus.hhplus_ecommerce.coupon.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.CouponRepository;
import com.hhplus.hhplus_ecommerce.coupon.repository.RedisCouponRepository;
import com.hhplus.hhplus_ecommerce.coupon.repository.RedisCouponRepository.CouponIssueRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Redis 기반 선착순 쿠폰 발급 서비스
 * - Atomic Operation으로 동시성 제어 (Lock 불필요)
 * - 비동기 대기열 처리로 빠른 응답 속도
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private final RedisCouponRepository redisCouponRepository;
    private final CouponRepository couponRepository;
    private final CouponTransactionService couponTransactionService;

    private static final int MAX_RETRY_COUNT = 3;


    public UserCoupon issueCouponWithRedis(Long userId, Long couponId) {
        log.info("[Redis 쿠폰 발급 시작] userId={}, couponId={}", userId, couponId);

        // 1. 쿠폰 존재 여부 확인
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        // 2. 쿠폰 유효성 확인
        if (!coupon.isValid()) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        // 3. 중복 발급 확인 (Redis Set - Atomic)
        Boolean isNew = redisCouponRepository.addIssuedUser(couponId, userId);
        if (!isNew) {
            log.warn("[중복 발급 시도] userId={}, couponId={}", userId, couponId);
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        try {

            // 5. DB에 저장 (트랜잭션)
            UserCoupon userCoupon = couponTransactionService.issueCouponTransaction(userId, couponId);
            log.info("[쿠폰 발급 성공] userCouponId={}, userId={}, couponId={}",
                    userCoupon.getId(), userId, couponId);

            return userCoupon;

        } catch (BusinessException e) {
            // 비즈니스 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            // 예상치 못한 예외 - Redis 롤백
            redisCouponRepository.removeIssuedUser(couponId, userId);
            log.error("[쿠폰 발급 실패 - Redis 롤백] userId={}, couponId={}", userId, couponId, e);
            throw new BusinessException(ErrorCode.COUPON_ISSUE_FAILED);
        }
    }


    public Long getIssuedCount(Long couponId) {
        return redisCouponRepository.getIssuedCount(couponId);
    }

    public void initializeCouponStock(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        // TTL 설정: 쿠폰 만료 시간까지
        Duration ttl = Duration.between(LocalDateTime.now(), coupon.getEndDate());

        // 유효기간 정보 Redis에 저장
        redisCouponRepository.setCouponValidity(couponId, coupon.getStartDate(), coupon.getEndDate(),coupon.getTotalQuantity(), ttl);
        redisCouponRepository.setExpire(couponId, ttl);

        log.info("[Redis 쿠폰 초기화] couponId={}, TTL={}일", couponId, ttl.toDays());
    }


    // ===== 비동기 대기열 방식 =====

    /**
     * 비동기 쿠폰 발급 요청 (대기열 추가)
     * - 빠른 응답: 대기열에 추가만 하고 즉시 반환
     * - 실제 발급: 스케줄러가 백그라운드에서 처리
     */
    public void requestCouponAsync(Long userId, Long couponId) {
        log.info("[비동기 쿠폰 발급 요청] userId={}, couponId={}", userId, couponId);

        // 1. 쿠폰 유효성 확인 (Redis)
        if (!redisCouponRepository.isCouponValid(couponId)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }


        // 2. 중복 발급 확인 (Redis Set - Atomic)
        Boolean isNew = redisCouponRepository.addIssuedUser(couponId, userId);
        if (!isNew) {
            log.warn("[중복 발급 시도] userId={}, couponId={}", userId, couponId);
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        // 3. 재고 확인 (빠른 실패)
        Integer totalQuantity = redisCouponRepository.getTotalQuantity(couponId);
        if (totalQuantity == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
        }

        Long issuedCount = redisCouponRepository.getIssuedCount(couponId);
        if (issuedCount >= totalQuantity) {
            redisCouponRepository.removeIssuedUser(couponId, userId);
            log.warn("[재고 부족] couponId={}, issuedCount={}, total={}", couponId, issuedCount, totalQuantity);
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }


        // 5. 대기열에 추가 (비동기 처리 대상)
        CouponIssueRequest request = new CouponIssueRequest(userId, couponId, 0);
        redisCouponRepository.addToQueue(couponId, request);
        log.info("[대기열 추가 성공] userId={}, couponId={}, queueSize={}",
                userId, couponId, redisCouponRepository.getQueueSize(couponId));
    }


    public int processCouponQueue(Long couponId, int batchSize) {
        log.info("[대기열 처리 시작] couponId={}, batchSize={}", couponId, batchSize);

        // 1. 대기열에서 요청 꺼내기
        List<CouponIssueRequest> requests = redisCouponRepository.popFromQueue(couponId, batchSize);
        if (requests.isEmpty()) {
            return 0;
        }

        // 2. 각 요청 처리
        int successCount = 0;
        int failCount = 0;

        for (CouponIssueRequest request : requests) {
            try {

                // DB에 저장
                couponTransactionService.issueCouponTransaction(request.userId(), request.couponId());
                successCount++;
                log.info("[쿠폰 발급 성공] userId={}, couponId={}", request.userId(), couponId);

            } catch (Exception e) {
                if(request.retryCount()<MAX_RETRY_COUNT){
                    CouponIssueRequest retryRequest = new CouponIssueRequest(
                            request.userId(),
                            request.couponId(),
                            request.retryCount()+1
                    );
                    redisCouponRepository.addToQueue(couponId, retryRequest);
                    log.warn("[재시도 예약] userId={}, retryCount={}", request.userId(), request.retryCount()+1);

                }else {
                    redisCouponRepository.removeIssuedUser(couponId, request.userId());
                    log.warn("[최대 재시도 초과] userId={}, couponId={}", request.userId(), couponId);
                }
                failCount++;
            }
        }

        log.info("[대기열 처리 완료] couponId={}, success={}, fail={}", couponId, successCount, failCount);
        return successCount;
    }

    public Long getQueueSize(Long couponId) {
        return redisCouponRepository.getQueueSize(couponId);
    }
}