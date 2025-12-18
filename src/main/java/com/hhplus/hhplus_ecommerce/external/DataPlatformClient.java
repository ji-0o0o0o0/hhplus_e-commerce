package com.hhplus.hhplus_ecommerce.external;

import com.hhplus.hhplus_ecommerce.external.dto.OrderDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class DataPlatformClient {

    
    public void sendOrderData(OrderDataDto orderData) {
        try {
            log.info("[데이터 플랫폼] 주문 정보 전송 시작 - OrderId: {}", orderData.getOrderId());

            // Mock: 외부 API 호출 지연 시뮬레이션 (1초)
            Thread.sleep(1000);

            log.info("[데이터 플랫폼] 전송 완료 - OrderId: {}, UserId: {}, FinalAmount: {}, Status: {}",
                    orderData.getOrderId(),
                    orderData.getUserId(),
                    orderData.getFinalAmount(),
                    orderData.getStatus());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[데이터 플랫폼] 전송 중 인터럽트 발생 - OrderId: {}", orderData.getOrderId(), e);
            throw new RuntimeException("데이터 플랫폼 전송 실패", e);
        } catch (Exception e) {
            log.warn("[데이터 플랫폼] 전송 실패 - OrderId: {}", orderData.getOrderId(), e);
            throw new RuntimeException("데이터 플랫폼 전송 실패", e);
        }
    }
}