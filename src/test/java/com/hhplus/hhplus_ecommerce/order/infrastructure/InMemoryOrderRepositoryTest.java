package com.hhplus.hhplus_ecommerce.order.infrastructure;

import com.hhplus.hhplus_ecommerce.order.OrderStatus;
import com.hhplus.hhplus_ecommerce.order.domain.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class InMemoryOrderRepositoryTest {

    private InMemoryOrderRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
    }

    @Test
    @DisplayName("새로운 주문을 저장할 수 있다")
    void save_신규_성공() {
        // given
        Order order = Order.create(1L, List.of(), null, 0L);

        // when
        Order saved = repository.save(order);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("기존 주문을 업데이트할 수 있다")
    void save_업데이트_성공() {
        // given
        Order order = Order.create(1L, List.of(), null, 0L);
        Order saved = repository.save(order);

        // when
        Order updated = Order.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .items(saved.getItems())
                .totalAmount(saved.getTotalAmount())
                .discountAmount(saved.getDiscountAmount())
                .finalAmount(saved.getFinalAmount())
                .status(OrderStatus.COMPLETED)
                .build();
        Order result = repository.save(updated);

        // then
        assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("ID로 주문을 조회할 수 있다")
    void findById_성공() {
        // given
        Order order = Order.create(1L, List.of(), null, 0L);
        Order saved = repository.save(order);

        // when
        Optional<Order> found = repository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 주문은 조회되지 않는다")
    void findById_없음() {
        // when
        Optional<Order> found = repository.findById(999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID로 주문 목록을 조회할 수 있다")
    void findByUserId_성공() {
        // given
        repository.save(Order.create(1L, List.of(), null, 0L));
        repository.save(Order.create(1L, List.of(), null, 0L));
        repository.save(Order.create(2L, List.of(), null, 0L));

        // when
        List<Order> orders = repository.findByUserId(1L);

        // then
        assertThat(orders).hasSize(2);
    }

    @Test
    @DisplayName("상태로 주문 목록을 조회할 수 있다")
    void findByStatus_성공() {
        // given
        Order order1 = Order.create(1L, List.of(), null, 0L);
        Order saved1 = repository.save(order1);

        Order order2 = Order.create(2L, List.of(), null, 0L);
        repository.save(order2);

        Order completed = Order.builder()
                .id(saved1.getId())
                .userId(saved1.getUserId())
                .items(saved1.getItems())
                .totalAmount(saved1.getTotalAmount())
                .discountAmount(saved1.getDiscountAmount())
                .finalAmount(saved1.getFinalAmount())
                .status(OrderStatus.COMPLETED)
                .build();
        repository.save(completed);

        // when
        List<Order> pendingOrders = repository.findByStatus(OrderStatus.PENDING);
        List<Order> completedOrders = repository.findByStatus(OrderStatus.COMPLETED);

        // then
        assertThat(pendingOrders).hasSize(1);
        assertThat(completedOrders).hasSize(1);
    }

    @Test
    @DisplayName("모든 주문을 조회할 수 있다")
    void findAll_성공() {
        // given
        repository.save(Order.create(1L, List.of(), null, 0L));
        repository.save(Order.create(2L, List.of(), null, 0L));

        // when
        List<Order> orders = repository.findAll();

        // then
        assertThat(orders).hasSize(2);
    }

    @Test
    @DisplayName("저장소를 초기화할 수 있다")
    void clear_성공() {
        // given
        repository.save(Order.create(1L, List.of(), null, 0L));
        repository.save(Order.create(2L, List.of(), null, 0L));

        // when
        repository.clear();

        // then
        assertThat(repository.findAll()).isEmpty();
    }
}