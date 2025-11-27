package com.hhplus.hhplus_ecommerce.coupon.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.common.lock.DistributedLockManager;
import com.hhplus.hhplus_ecommerce.common.lock.RedisLockKey;
import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.dto.response.CouponIssueResponse;
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

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final DistributedLockManager lockManager;
    private final CouponTransactionService couponTransactionService;

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


    public UserCoupon issueCouponWithDistributedLock(Long userId, Long couponId) {
        String lockKey = RedisLockKey.couponIssue(couponId);
        return lockManager.executeWithLock(lockKey, 5L, 10L, () ->
                couponTransactionService.issueCouponTransaction(userId, couponId)
        );
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
        return couponRepository.save(coupon);
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

}
