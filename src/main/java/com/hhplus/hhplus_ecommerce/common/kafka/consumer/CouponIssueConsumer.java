package com.hhplus.hhplus_ecommerce.common.kafka.consumer;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.common.kafka.KafkaTopics;
import com.hhplus.hhplus_ecommerce.common.kafka.message.CouponIssueRequestMessage;
import com.hhplus.hhplus_ecommerce.coupon.domain.Coupon;
import com.hhplus.hhplus_ecommerce.coupon.domain.UserCoupon;
import com.hhplus.hhplus_ecommerce.coupon.repository.CouponRepository;
import com.hhplus.hhplus_ecommerce.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    /**
     * 쿠폰 발급 요청 메시지 소비
     * - 토픽: coupon-issue-request
     * - 컨슈머 그룹: coupon-issue-service
     * - 수동 커밋: 처리 완료 후 명시적으로 커밋
     * - 멱등성: 중복 발급 방지
     */
    @KafkaListener(
            topics = KafkaTopics.COUPON_ISSUE_REQUEST,
            groupId = "coupon-issue-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCouponIssueRequest(
            @Payload CouponIssueRequestMessage message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            log.info("[Kafka Consumer] 쿠폰 발급 요청 수신 - Partition: {}, Offset: {}, RequestId: {}, UserId: {}, CouponId: {}",
                    partition, offset, message.getRequestId(), message.getUserId(), message.getCouponId());

            // 쿠폰 발급 처리
            issueCoupon(message);

            // 처리 완료 후 명시적 커밋
            acknowledgment.acknowledge();

            log.info("[Kafka Consumer] 쿠폰 발급 처리 완료 - Partition: {}, Offset: {}, RequestId: {}",
                    partition, offset, message.getRequestId());

        } catch (BusinessException e) {
            // 비즈니스 예외 (재고 없음, 중복 발급 등) - 커밋하고 다음 메시지 처리
            log.warn("[Kafka Consumer] 쿠폰 발급 실패 (비즈니스 예외) - Partition: {}, Offset: {}, RequestId: {}, Error: {}",
                    partition, offset, message.getRequestId(), e.getMessage());
            acknowledgment.acknowledge();

        } catch (Exception e) {
            // 시스템 예외 - 커밋하지 않음, 재처리됨
            log.warn("[Kafka Consumer] 쿠폰 발급 처리 실패 (시스템 예외) - Partition: {}, Offset: {}, RequestId: {}, Error: {}",
                    partition, offset, message.getRequestId(), e.getMessage(), e);
            // TODO: 재시도 3회 초과 시 DLT로 전송
        }
    }

    /**
     * 쿠폰 발급 로직
     * - 중복 발급 확인
     * - 쿠폰 재고 확인 및 차감
     * - UserCoupon 생성
     */
    @Transactional
    protected void issueCoupon(CouponIssueRequestMessage message) {
        Long userId = message.getUserId();
        Long couponId = message.getCouponId();

        // 1. 중복 발급 확인
        userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                .ifPresent(uc -> {
                    throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
                });

        // 2. 쿠폰 조회
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        // 3. 쿠폰 발급 가능 여부 확인
        if (!coupon.canIssue()) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
        }

        if (!coupon.isValid()) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        // 4. 쿠폰 재고 차감
        coupon.increaseIssuedQuantity();
        couponRepository.save(coupon);

        // 5. UserCoupon 생성
        UserCoupon userCoupon = UserCoupon.issue(userId, coupon);
        userCouponRepository.save(userCoupon);

        log.info("[쿠폰 발급] 발급 완료 - UserId: {}, CouponId: {}, UserCouponId: {}",
                userId, couponId, userCoupon.getId());
    }
}