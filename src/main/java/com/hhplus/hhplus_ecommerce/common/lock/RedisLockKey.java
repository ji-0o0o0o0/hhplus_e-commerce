package com.hhplus.hhplus_ecommerce.common.lock;

/**
 * 키 네이밍 컨벤션: {service}:{domain}:{function}:{id}
 */
public class RedisLockKey {

    private static final String SERVICE_PREFIX = "ecommerce";
    private static final String LOCK_PREFIX = "lock";

    private static final String PRODUCT_DOMAIN = "product";

    public static String productStock(Long productId) {
        return buildKey(PRODUCT_DOMAIN, "stock", productId.toString());
    }

    private static final String POINT_DOMAIN = "point";

    public static String pointTransaction(Long userId) {
        return buildKey(POINT_DOMAIN, "transaction", userId.toString());
    }

    private static final String COUPON_DOMAIN = "coupon";

    public static String couponIssue(Long couponId) {
        return buildKey(COUPON_DOMAIN, "issue", couponId.toString());
    }


    private static String buildKey(String domain, String function, String id) {
        return String.join(":", SERVICE_PREFIX, LOCK_PREFIX, domain, function, id);
    }
}
