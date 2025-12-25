package com.hhplus.hhplus_ecommerce.coupon.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.common.kafka.message.CouponIssueRequestMessage;
import com.hhplus.hhplus_ecommerce.common.kafka.producer.KafkaProducerService;
import com.hhplus.hhplus_ecommerce.common.lock.DistributedLockManager;
import com.hhplus.hhplus_ecommerce.common.lock.RedisLockKey;
import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponIssueResponse;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponIssueStatusResponse;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponListResponse;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.UserCouponDto;
import com.hhplus.hhplus_ecommerce.coupon.repository.CouponRepository;
import com.hhplus.hhplus_ecommerce.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final DistributedLockManager lockManager;
    private final CouponTransactionService couponTransactionService;
    private final CouponRedisService couponRedisService;
    private final KafkaProducerService kafkaProducerService;

    //낙관적락(성능 비교용)
    @Transactional
    @Retryable(
            retryFor = {org.springframework.orm.ObjectOptimisticLockingFailureException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 50)
    )
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                .ifPresent(uc -> {
                    throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
                });

        if (!coupon.canIssue()) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }

        if (!coupon.isValid()) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        coupon.increaseIssuedQuantity();
        couponRepository.save(coupon);

        UserCoupon userCoupon = UserCoupon.issue(userId, coupon);
        return userCouponRepository.save(userCoupon);
    }


    // 동기 처리 (분산락 사용) - 성능 비교용
    public UserCoupon issueCouponWithDistributedLock(Long userId, Long couponId) {
        String lockKey = RedisLockKey.couponIssue(couponId);
        return lockManager.executeWithLock(lockKey, 5L, 10L, () ->
                couponTransactionService.issueCouponTransaction(userId, couponId)
        );
    }

    /**
     * 비동기 쿠폰 발급 (Kafka 사용)
     * - Redis에서 빠른 재고 확인
     * - Kafka에 발급 요청 메시지 발행
     * - 즉시 응답 반환 (202 Accepted)
     * - Consumer가 실제 발급 처리
     */
    public String issueCouponAsync(Long userId, Long couponId) {
        // 1. 쿠폰 존재 여부 및 유효성 확인
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        if (!coupon.isValid()) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        // 2. Redis에서 빠른 재고 확인 (최종 검증은 Consumer에서)
        if (!coupon.canIssue()) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }

        // 3. 고유한 요청 ID 생성 (멱등성 키)
        String requestId = UUID.randomUUID().toString();

        // 4. Kafka에 쿠폰 발급 요청 메시지 발행
        CouponIssueRequestMessage message = new CouponIssueRequestMessage(
                requestId,
                userId,
                couponId,
                LocalDateTime.now()
        );

        kafkaProducerService.sendCouponIssueRequest(message);

        return requestId;
    }

    public List<UserCoupon> getUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);

        for (UserCoupon userCoupon : userCoupons) {
            if (userCoupon.shouldExpire()) {
                userCoupon.expire();
                userCouponRepository.save(userCoupon);
            }
        }

        return userCoupons;
    }

    public List<UserCoupon> getAvailableUserCoupons(Long userId) {
        return userCouponRepository.findByUserIdAndStatus(userId, CouponStatus.AVAILABLE);
    }

    public List<Coupon> getAvailableCoupons() {
        return couponRepository.findAvailableCoupons();
    }

    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
    }

    public Coupon createCoupon(String name, Integer discountRate, Integer totalQuantity,Integer validityDays,
                               LocalDateTime startDate, LocalDateTime endDate) {
        Coupon coupon = Coupon.create(name, discountRate, totalQuantity,validityDays, startDate, endDate);
        Coupon savedCoupon = couponRepository.save(coupon);
        couponRedisService.initializeCouponStock(savedCoupon.getId());
        return savedCoupon;
    }

    public CouponIssueResponse issueCouponWithResponse(UserCoupon issueCoupon) {
        Coupon coupon = getCoupon(issueCoupon.getCouponId());

        return new CouponIssueResponse(
                issueCoupon.getId(),
                issueCoupon.getUserId(),
                issueCoupon.getCouponId(),
                coupon.getName(),
                coupon.getDiscountRate(),
                issueCoupon.getStatus(),
                issueCoupon.getIssuedAt(),
                issueCoupon.getExpiresAt()
        );
    }

    @Cacheable(value = "userCoupons", key = "#userId + '_' + (#status != null ? #status : 'ALL')", sync = true)
    public CouponListResponse getUserCouponsWithDetails(Long userId, CouponStatus status) {
        List<UserCoupon> userCoupons;
        if (status != null) {
            userCoupons = userCouponRepository.findByUserIdAndStatus(userId, status);
        } else {
            userCoupons = getUserCoupons(userId);
        }

        List<UserCouponDto> dtos = userCoupons.stream()
                .map(uc -> {
                    Coupon coupon = getCoupon(uc.getCouponId());
                    return new UserCouponDto(
                            uc.getId(),
                            uc.getCouponId(),
                            coupon.getName(),
                            coupon.getDiscountRate(),
                            uc.getStatus(),
                            uc.getIssuedAt(),
                            uc.getUsedAt(),
                            uc.getExpiresAt()
                    );
                })
                .toList();

        return new CouponListResponse(dtos);
    }

    /**
     * 비동기 쿠폰 발급 상태 조회
     * - requestId로 발급 결과 확인
     * - DB에서 영구 추적 가능
     */
    public CouponIssueStatusResponse getCouponIssueStatus(String requestId) {
        return userCouponRepository.findByRequestId(requestId)
                .map(uc -> CouponIssueStatusResponse.completed(
                        requestId,
                        uc.getUserId(),
                        uc.getCouponId(),
                        uc.getId(),
                        uc.getName()
                ))
                .orElse(CouponIssueStatusResponse.notFound(requestId));
    }

}
