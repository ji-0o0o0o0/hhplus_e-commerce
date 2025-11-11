package com.hhplus.hhplus_ecommerce.point.application;

import com.hhplus.hhplus_ecommerce.point.TransactionType;
import com.hhplus.hhplus_ecommerce.point.domain.Point;
import com.hhplus.hhplus_ecommerce.point.domain.PointTransaction;
import com.hhplus.hhplus_ecommerce.point.repository.PointRepository;
import com.hhplus.hhplus_ecommerce.point.repository.PointTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @InjectMocks
    private PointService pointService;

    private Long userId;
    private Point point;

    @BeforeEach
    void setUp() {
        userId = 1L;
        point = Point.builder()
                .id(1L)
                .userId(userId)
                .amount(10000)
                .build();
    }

    @Test
    @DisplayName("포인트를 조회할 수 있다")
    void getPoint_성공() {
        // given
        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(point));

        // when
        Point result = pointService.getPoint(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(10000);
    }

    @Test
    @DisplayName("포인트가 없으면 0원으로 생성한다")
    void getPoint_생성() {
        // given
        given(pointRepository.findByUserId(userId)).willReturn(Optional.empty());
        Point newPoint = Point.create(userId);
        given(pointRepository.save(any(Point.class))).willReturn(newPoint);

        // when
        Point result = pointService.getPoint(userId);

        // then
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getAmount()).isEqualTo(0),
                () -> assertThat(result.getUserId()).isEqualTo(userId)
        );
        verify(pointRepository).save(any(Point.class));
    }

    @Test
    @DisplayName("포인트를 충전할 수 있다")
    void chargePoint_성공() {
        // given
        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(point));
        given(pointRepository.save(any(Point.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(pointTransactionRepository.save(any(PointTransaction.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        Point result = pointService.changePoint(userId, 5000);

        // then
        assertThat(result.getAmount()).isEqualTo(15000);
        verify(pointRepository).save(point);
        verify(pointTransactionRepository).save(any(PointTransaction.class));
    }

    @Test
    @DisplayName("포인트를 사용할 수 있다")
    void usePoint_성공() {
        // given
        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(point));
        given(pointRepository.save(any(Point.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(pointTransactionRepository.save(any(PointTransaction.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        Point result = pointService.usePoint(userId, 3000);

        // then
        assertThat(result.getAmount()).isEqualTo(7000);
        verify(pointRepository).save(point);
        verify(pointTransactionRepository).save(any(PointTransaction.class));
    }

    @Test
    @DisplayName("포인트 충전 시 거래 내역이 저장된다")
    void chargePoint_거래내역저장() {
        // given
        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(point));
        given(pointRepository.save(any(Point.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        pointService.changePoint(userId, 5000);

        // then
        verify(pointTransactionRepository).save(argThat(transaction ->
                transaction.getType() == TransactionType.CHARGE &&
                        transaction.getAmount() == 5000 &&
                        transaction.getUserId().equals(userId)
        ));
    }

    @Test
    @DisplayName("포인트 사용 시 거래 내역이 저장된다")
    void usePoint_거래내역저장() {
        // given
        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(point));
        given(pointRepository.save(any(Point.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        pointService.usePoint(userId, 3000);

        // then
        verify(pointTransactionRepository).save(argThat(transaction ->
                transaction.getType() == TransactionType.USE &&
                        transaction.getAmount() == 3000 &&
                        transaction.getUserId().equals(userId)
        ));
    }

    @Test
    @DisplayName("거래 내역을 조회할 수 있다")
    void getPointTransactions_성공() {
        // given
        PointTransaction transaction1 = PointTransaction.create(userId, 5000, TransactionType.CHARGE, 5000);
        PointTransaction transaction2 = PointTransaction.create(userId, 2000, TransactionType.USE, 3000);
        given(pointTransactionRepository.findByUserId(userId))
                .willReturn(List.of(transaction1, transaction2));

        // when
        List<PointTransaction> result = pointService.getPointTransactions(userId);

        // then
        assertThat(result).hasSize(2);
    }
}