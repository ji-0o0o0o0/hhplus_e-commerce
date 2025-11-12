package com.hhplus.hhplus_ecommerce.coupon.infrastructure;

import com.hhplus.hhplus_ecommerce.coupon.CouponStatus;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.UserCouponRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryUserCouponRepository implements UserCouponRepository {

    private final Map<Long, UserCoupon> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        if (userCoupon.getId() == null) {
            UserCoupon newUserCoupon = UserCoupon.builder()
                    .id(idGenerator.getAndIncrement())
                    .userId(userCoupon.getUserId())
                    .couponId(userCoupon.getCouponId())
                    .name(userCoupon.getName())
                    .discountRate(userCoupon.getDiscountRate())
                    .status(userCoupon.getStatus())
                    .issuedAt(userCoupon.getIssuedAt())
                    .usedAt(userCoupon.getUsedAt())
                    .expiresAt(userCoupon.getExpiresAt())
                    .build();
            store.put(newUserCoupon.getId(), newUserCoupon);
            return newUserCoupon;
        } else {
            store.put(userCoupon.getId(), userCoupon);
            return userCoupon;
        }
    }

    @Override
    public Optional<UserCoupon> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<UserCoupon> findByUserId(Long userId) {
        return store.values().stream()
                .filter(uc -> userId.equals(uc.getUserId()))
               .toList();
    }

    @Override
    public Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId) {
        return store.values().stream()
                .filter(uc -> userId.equals(uc.getUserId()) && couponId.equals(uc.getCouponId()))
                .findFirst();
    }

    @Override
    public List<UserCoupon> findByUserIdAndStatus(Long userId, CouponStatus status) {
        return store.values().stream()
                .filter(uc -> userId.equals(uc.getUserId()) && status.equals(uc.getStatus()))
               .toList();
    }

    public void clear() {
        store.clear();
        idGenerator.set(1);
    }
}