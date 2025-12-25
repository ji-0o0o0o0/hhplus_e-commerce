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
     * - 수동 커밋: 성공 시에만 명시적 커밋
     * - 멱등성: 중복 발급 방지
     * - 재시도 정책: 재시도 불가능한 비즈니스 예외만 ack 처리
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

            // 성공 시에만 명시적 커밋
            acknowledgment.acknowledge();

            log.info("[Kafka Consumer] 쿠폰 발급 처리 완료 - Partition: {}, Offset: {}, RequestId: {}",
                    partition, offset, message.getRequestId());

        } catch (BusinessException e) {
            // 재시도 불필요한 비즈니스 예외 (중복 발급, 쿠폰 없음 등)
            if (isNonRetryableError(e.getErrorCode())) {
                log.warn("[Kafka Consumer] 재시도 불필요한 비즈니스 예외 - ack 처리 - Partition: {}, Offset: {}, RequestId: {}, ErrorCode: {}, Message: {}",
                        partition, offset, message.getRequestId(), e.getErrorCode(), e.getMessage());
                acknowledgment.acknowledge();
            } else {
                // 재시도 가능한 비즈니스 예외 (일시적 재고 부족 등)
                log.warn("[Kafka Consumer] 재시도 가능한 비즈니스 예외 - 재처리 예정 - Partition: {}, Offset: {}, RequestId: {}, ErrorCode: {}, Message: {}",
                        partition, offset, message.getRequestId(), e.getErrorCode(), e.getMessage());
                // ack 호출하지 않음 → 재처리됨
            }

        } catch (Exception e) {
            // 시스템 예외 - ack 호출하지 않음, 재처리됨
            log.error("[Kafka Consumer] 시스템 예외 발생 - 재처리 예정 - Partition: {}, Offset: {}, RequestId: {}, Error: {}",
                    partition, offset, message.getRequestId(), e.getMessage(), e);
            // TODO: 재시도 3회 초과 시 DLT(Dead Letter Topic)로 전송
        }
    }

    /**
     * 재시도가 불필요한 비즈니스 예외인지 판단
     * - 재시도해도 결과가 바뀌지 않는 예외는 ack 처리하여 무한 재시도 방지
     */
    private boolean isNonRetryableError(ErrorCode errorCode) {
        return errorCode == ErrorCode.COUPON_ALREADY_ISSUED    // 이미 발급됨 (멱등성)
                || errorCode == ErrorCode.COUPON_NOT_FOUND      // 쿠폰이 존재하지 않음
                || errorCode == ErrorCode.COUPON_NOT_AVAILABLE  // 쿠폰이 유효하지 않음
                || errorCode == ErrorCode.COUPON_SOLD_OUT;      // 재고 소진
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
        String requestId = message.getRequestId();

        // 1. 멱등성 체크 (같은 requestId로 이미 발급했는지 확인)
        if (userCouponRepository.findByRequestId(requestId).isPresent()) {
            log.info("[쿠폰 발급] 이미 처리된 요청 - RequestId: {}", requestId);
            return; // 멱등성 보장: 이미 처리된 요청은 성공으로 처리
        }

        // 2. 중복 발급 확인 (같은 쿠폰을 여러 번 발급받는지 체크)
        userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                .ifPresent(uc -> {
                    throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
                });

        // 3. 쿠폰 조회
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

        // 5. UserCoupon 생성 (requestId 포함)
        UserCoupon userCoupon = UserCoupon.issue(userId, coupon, message.getRequestId());
        userCouponRepository.save(userCoupon);

        log.info("[쿠폰 발급] 발급 완료 - UserId: {}, CouponId: {}, UserCouponId: {}, RequestId: {}",
                userId, couponId, userCoupon.getId(), message.getRequestId());
    }
}