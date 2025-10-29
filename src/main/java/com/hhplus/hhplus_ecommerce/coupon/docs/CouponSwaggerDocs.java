package com.hhplus.hhplus_ecommerce.coupon.docs;

public class CouponSwaggerDocs {

    public static final String ISSUE_COUPON_SUCCESS = """
            {
                "code": 201,
                "message": "쿠폰이 성공적으로 발급되었습니다",
                "data": {
                    "userCouponId": 1,
                    "userId": 1,
                    "couponId": 1,
                    "couponName": "20% 할인 쿠폰",
                    "discountRate": 20,
                    "status": "AVAILABLE",
                    "issuedAt": "2025-10-30T10:00:00",
                    "expiresAt": "2025-11-30T10:00:00"
                }
            }
            """;

    public static final String COUPON_NOT_FOUND = """
            {
                "code": 404,
                "message": "쿠폰을 찾을 수 없습니다.",
                "data": null
            }
            """;

    public static final String ALREADY_ISSUED = """
            {
                "code": 409,
                "message": "이미 발급받은 쿠폰입니다.",
                "data": null
            }
            """;

    public static final String COUPON_SOLD_OUT = """
            {
                "code": 409,
                "message": "쿠폰이 모두 소진되었습니다.",
                "data": null
            }
            """;
}