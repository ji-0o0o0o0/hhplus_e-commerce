package com.hhplus.hhplus_ecommerce.point.infrastructure;

import com.hhplus.hhplus_ecommerce.point.TransactionType;
import com.hhplus.hhplus_ecommerce.point.domain.PointTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class InMemoryPointTransactionRepositoryTest {

    private InMemoryPointTransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPointTransactionRepository();
    }

    @Test
    @DisplayName("새로운 포인트 거래를 저장할 수 있다")
    void save_신규_성공() {
        // given
        PointTransaction transaction = PointTransaction.create(1L, 1000L, TransactionType.CHARGE, 1000L);

        // when
        PointTransaction saved = repository.save(transaction);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPointId()).isEqualTo(1L);
        assertThat(saved.getAmount()).isEqualTo(1000);
        assertThat(saved.getType()).isEqualTo(TransactionType.CHARGE);
        assertThat(saved.getBalanceAfter()).isEqualTo(1000);
    }

    @Test
    @DisplayName("기존 포인트 거래를 업데이트할 수 있다")
    void save_업데이트_성공() {
        // given
        PointTransaction transaction = PointTransaction.create(1L, 1000L, TransactionType.CHARGE, 1000L);
        PointTransaction saved = repository.save(transaction);

        // when
        PointTransaction updated = PointTransaction.builder()
                .id(saved.getId())
                .pointId(saved.getPointId())
                .type(saved.getType())
                .amount(saved.getAmount())
                .balanceAfter(2000L)
                .createdAt(saved.getCreatedAt())
                .build();
        PointTransaction result = repository.save(updated);

        // then
        assertThat(result.getBalanceAfter()).isEqualTo(2000);
    }

    @Test
    @DisplayName("사용자 ID로 거래 내역을 조회할 수 있다")
    void findByUserId_성공() {
        // given
        repository.save(PointTransaction.create(1L, 1000L, TransactionType.CHARGE, 1000L));
        repository.save(PointTransaction.create(1L, 500L, TransactionType.USE, 500L));
        repository.save(PointTransaction.create(2L, 2000L, TransactionType.CHARGE, 2000L));

        // when
        List<PointTransaction> transactions = repository.findByPointId(1L);

        // then
        assertThat(transactions).hasSize(2);
    }

    @Test
    @DisplayName("사용자 ID로 거래 내역을 페이징하여 조회할 수 있다")
    void findByUserId_페이징_성공() {
        // given
        repository.save(PointTransaction.create(1L, 1000L, TransactionType.CHARGE, 1000L));
        repository.save(PointTransaction.create(1L, 500L, TransactionType.USE, 500L));
        repository.save(PointTransaction.create(1L, 300L, TransactionType.CHARGE, 800L));
        repository.save(PointTransaction.create(1L, 100L, TransactionType.USE, 700L));

        // when
        List<PointTransaction> page1 = repository.findByPointId(1L, 0, 2);
        List<PointTransaction> page2 = repository.findByPointId(1L, 2, 2);

        // then
        assertThat(page1).hasSize(2);
        assertThat(page2).hasSize(2);
    }

    @Test
    @DisplayName("거래 내역은 최신순으로 정렬된다")
    void findByUserId_최신순정렬() throws InterruptedException {
        // given
        PointTransaction t1 = repository.save(PointTransaction.create(1L, 1000L, TransactionType.CHARGE, 1000L));
        Thread.sleep(10);  // 시간 차이를 위해
        PointTransaction t2 = repository.save(PointTransaction.create(1L, 500L, TransactionType.USE, 500L));

        // when
        List<PointTransaction> transactions = repository.findByPointId(1L);

        // then
        assertThat(transactions.get(0).getId()).isEqualTo(t2.getId());  // 최신 거래가 먼저
        assertThat(transactions.get(1).getId()).isEqualTo(t1.getId());
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 거래 내역은 빈 목록이다")
    void findByUserId_없음() {
        // when
        List<PointTransaction> transactions = repository.findByPointId(999L);

        // then
        assertThat(transactions).isEmpty();
    }

    @Test
    @DisplayName("저장소를 초기화할 수 있다")
    void clear_성공() {
        // given
        repository.save(PointTransaction.create(1L, 1000L, TransactionType.CHARGE, 1000L));
        repository.save(PointTransaction.create(2L, 2000L, TransactionType.CHARGE, 2000L));

        // when
        repository.clear();

        // then
        assertThat(repository.findByPointId(1L)).isEmpty();
        assertThat(repository.findByPointId(2L)).isEmpty();
    }
}