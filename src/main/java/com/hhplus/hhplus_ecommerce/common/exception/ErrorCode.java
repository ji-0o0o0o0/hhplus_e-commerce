package com.hhplus.hhplus_ecommerce.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 공통
    INVALID_INPUT_VALUE("INVALID_INPUT_VALUE", 400, "입력값이 올바르지 않습니다"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", 500, "서버 오류가 발생했습니다"),

    // 포인트
    POINT_INSUFFICIENT_BALANCE("POINT_INSUFFICIENT_BALANCE", 400, "포인트 잔액이 부족합니다"),
    POINT_INVALID_CHARGE_AMOUNT("POINT_INVALID_CHARGE_AMOUNT", 400, "충전 금액은 0보다 커야 합니다"),
    POINT_INVALID_USE_AMOUNT("POINT_INVALID_USE_AMOUNT", 400, "사용 금액은 0보다 커야 합니다"),
    POINT_CHARGE_AMOUNT_EXCEEDS_ONCE("POINT_CHARGE_AMOUNT_EXCEEDS_ONCE", 409, "1회 최대 충전 금액을 초과했습니다"),
    POINT_MAX_BALANCE_EXCEEDED("POINT_MAX_BALANCE_EXCEEDED", 400, "최대 보유 가능한 포인트를 초과했습니다"),
    POINT_NOT_FOUND("POINT_NOT_FOUND", 404, "포인트 정보를 찾을 수 없습니다"),

    // 상품
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", 404, "상품을 찾을 수 없습니다"),
    PRODUCT_INSUFFICIENT_STOCK("PRODUCT_INSUFFICIENT_STOCK", 400, "상품의 재고가 부족합니다"),
    PRODUCT_INVALID_STOCK("PRODUCT_INVALID_STOCK", 400, "재고는 0 이상이어야 합니다"),
    PRODUCT_INVALID_PRICE("PRODUCT_INVALID_PRICE", 400, "가격은 0 이상이어야 합니다"),
    PRODUCT_INVALID_NAME("PRODUCT_INVALID_NAME", 400, "이름은 필수로 기재해야 합니다"),

    // 쿠폰
    COUPON_NOT_FOUND("COUPON_NOT_FOUND", 404, "쿠폰을 찾을 수 없습니다"),
    COUPON_ALREADY_ISSUED("COUPON_ALREADY_ISSUED", 409, "이미 발급받은 쿠폰입니다"),
    COUPON_SOLD_OUT("COUPON_SOLD_OUT", 409, "쿠폰이 모두 소진되었습니다"),
    COUPON_NOT_AVAILABLE("COUPON_NOT_AVAILABLE", 400, "사용할 수 없는 쿠폰입니다"),

    // 장바구니
    CART_ITEM_NOT_FOUND("CART_ITEM_NOT_FOUND", 404, "장바구니 항목을 찾을 수 없습니다"),
    CART_EMPTY("CART_EMPTY", 400, "장바구니가 비어있습니다"),

    // 주문
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", 404, "주문을 찾을 수 없습니다"),
    ORDER_CANNOT_CANCEL("ORDER_CANNOT_CANCEL", 400, "취소할 수 없는 주문입니다"),

    // 결제
    PAYMENT_NOT_FOUND("PAYMENT_NOT_FOUND", 404, "결제 정보를 찾을 수 없습니다"),
    PAYMENT_FAILED("PAYMENT_FAILED", 400, "결제에 실패했습니다"),
    PAYMENT_ALREADY_COMPLETED("PAYMENT_ALREADY_COMPLETED", 409, "이미 완료된 결제입니다");

    private final String code;
    private final int status;
    private final String message;

    ErrorCode(String code, int status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}