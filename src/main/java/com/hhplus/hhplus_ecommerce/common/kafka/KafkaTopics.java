package com.hhplus.hhplus_ecommerce.common.kafka;


public final class KafkaTopics {

    private KafkaTopics() {
    }

    /**
     * 주문 완료 이벤트 토픽
     * - 파티션 6개 (병렬 처리)
     * - userId를 키로 사용하여 같은 사용자의 주문은 순서 보장
     */
    public static final String ORDER_COMPLETED = "order-completed";

    /**
     * 결제 완료 이벤트 토픽
     * - 파티션 6개 (병렬 처리)
     * - userId를 키로 사용하여 같은 사용자의 결제는 순서 보장
     */
    public static final String PAYMENT_COMPLETED = "payment-completed";
}