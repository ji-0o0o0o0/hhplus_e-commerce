package com.hhplus.hhplus_ecommerce.order.docs;

public class OrderSwaggerDocs {

    public static final String CREATE_ORDER_SUCCESS = """
            {
                "code": 201,
                "message": "주문이 생성되었습니다.",
                "data": {
                    "orderId": 1,
                    "userId": 1,
                    "totalAmount": 2000000,
                    "discountAmount": 200000,
                    "finalAmount": 1800000,
                    "status": "PENDING",
                    "orderItems": [
                        {
                            "orderItemId": 1,
                            "productId": 1,
                            "productName": "Macbook Pro",
                            "quantity": 1,
                            "unitPrice": 2000000,
                            "subtotal": 2000000
                        }
                    ],
                    "createdAt": "2025-10-30T10:00:00"
                }
            }
            """;

    public static final String DIRECT_ORDER_WITHOUT_ITEMS = """
            {
                "code": 400,
                "message": "즉시 구매 시 상품 목록은 필수입니다.",
                "data": null
            }
            """;

    public static final String COUPON_NOT_FOUND = """
            {
                "code": 404,
                "message": "쿠폰을 찾을 수 없습니다.",
                "data": null
            }
            """;

    public static final String PRODUCT_OUT_OF_STOCK = """
            {
                "code": 400,
                "message": "상품의 재고가 부족합니다.",
                "data": null
            }
            """;
}