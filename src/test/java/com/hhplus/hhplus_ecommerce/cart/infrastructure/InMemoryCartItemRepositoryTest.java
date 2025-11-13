package com.hhplus.hhplus_ecommerce.cart.infrastructure;

import com.hhplus.hhplus_ecommerce.cart.domain.CartItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class InMemoryCartItemRepositoryTest {

    private InMemoryCartItemRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCartItemRepository();
    }

    @Test
    @DisplayName("새로운 장바구니 항목을 저장할 수 있다")
    void save_신규_성공() {
        // given
        CartItem item = CartItem.create(1L, 10L, 2);

        // when
        CartItem saved = repository.save(item);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getProductId()).isEqualTo(10L);
        assertThat(saved.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("기존 장바구니 항목을 업데이트할 수 있다")
    void save_업데이트_성공() {
        // given
        CartItem item = CartItem.create(1L, 10L, 2);
        CartItem saved = repository.save(item);

        // when
        CartItem updated = CartItem.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .productId(saved.getProductId())
                .quantity(5)
                .build();
        CartItem result = repository.save(updated);

        // then
        assertThat(result.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("ID로 장바구니 항목을 조회할 수 있다")
    void findById_성공() {
        // given
        CartItem item = CartItem.create(1L, 10L, 2);
        CartItem saved = repository.save(item);

        // when
        Optional<CartItem> found = repository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getProductId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("존재하지 않는 장바구니 항목은 조회되지 않는다")
    void findById_없음() {
        // when
        Optional<CartItem> found = repository.findById(999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID로 장바구니 항목 목록을 조회할 수 있다")
    void findByUserId_성공() {
        // given
        repository.save(CartItem.create(1L, 10L, 2));
        repository.save(CartItem.create(1L, 11L, 1));
        repository.save(CartItem.create(2L, 10L, 3));

        // when
        List<CartItem> items = repository.findByUserId(1L);

        // then
        assertThat(items).hasSize(2);
    }

    @Test
    @DisplayName("사용자 ID와 상품 ID로 장바구니 항목을 조회할 수 있다")
    void findByUserIdAndProductId_성공() {
        // given
        repository.save(CartItem.create(1L, 10L, 2));
        repository.save(CartItem.create(1L, 11L, 1));

        // when
        Optional<CartItem> found = repository.findByUserIdAndProductId(1L, 10L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("존재하지 않는 조합은 조회되지 않는다")
    void findByUserIdAndProductId_없음() {
        // when
        Optional<CartItem> found = repository.findByUserIdAndProductId(1L, 999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("장바구니 항목을 삭제할 수 있다")
    void delete_성공() {
        // given
        CartItem item = CartItem.create(1L, 10L, 2);
        CartItem saved = repository.save(item);

        // when
        repository.delete(saved.getId());

        // then
        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("사용자의 모든 장바구니 항목을 삭제할 수 있다")
    void deleteAllByUserId_성공() {
        // given
        repository.save(CartItem.create(1L, 10L, 2));
        repository.save(CartItem.create(1L, 11L, 1));
        repository.save(CartItem.create(2L, 10L, 3));

        // when
        repository.deleteAllByUserId(1L);

        // then
        assertThat(repository.findByUserId(1L)).isEmpty();
        assertThat(repository.findByUserId(2L)).hasSize(1);
    }

    @Test
    @DisplayName("저장소를 초기화할 수 있다")
    void clear_성공() {
        // given
        repository.save(CartItem.create(1L, 10L, 2));
        repository.save(CartItem.create(2L, 11L, 1));

        // when
        repository.clear();

        // then
        assertThat(repository.findByUserId(1L)).isEmpty();
        assertThat(repository.findByUserId(2L)).isEmpty();
    }
}