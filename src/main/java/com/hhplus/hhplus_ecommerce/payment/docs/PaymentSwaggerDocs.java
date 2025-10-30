package com.hhplus.hhplus_ecommerce.payment.docs;

public class PaymentSwaggerDocs {

    public static final String PAYMENT_SUCCESS = """
            {
                "code": 200,
                "message": "요청이 정상적으로 처리되었습니다.",
                "data": {
                    "paymentId": 1,
                    "orderId": 1,
                    "userId": 1,
                    "finalAmount": 50000,
                    "balanceAfter": 50000,
                    "status": "COMPLETED",
                    "paidAt": "2025-10-30T10:00:00"
                }
            }
            """;

    public static final String ORDER_NOT_FOUND = """
            {
                "code": 404,
                "message": "주문을 찾을 수 없습니다.",
                "data": null
            }
            """;

    public static final String INSUFFICIENT_BALANCE = """
            {
                "code": 400,
                "message": "잔액이 부족합니다.",
                "data": null
            }
            """;
}