package com.hhplus.hhplus_ecommerce.point.infrastructure;

import com.hhplus.hhplus_ecommerce.point.domain.Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class InMemoryPointRepositoryTest {

    private InMemoryPointRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPointRepository();
    }

    @Test
    @DisplayName("새로운 포인트를 저장할 수 있다")
    void save_신규_성공() {
        // given
        Point point = Point.create(1L);

        // when
        Point saved = repository.save(point);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getAmount()).isEqualTo(0);
    }

    @Test
    @DisplayName("기존 포인트를 업데이트할 수 있다")
    void save_업데이트_성공() {
        // given
        Point point = Point.create(1L);
        Point saved = repository.save(point);

        // when
        Point updated = Point.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .amount(10000)
                .updatedAt(LocalDateTime.now())
                .build();
        Point result = repository.save(updated);

        // then
        assertThat(result.getAmount()).isEqualTo(10000);
    }

    @Test
    @DisplayName("ID로 포인트를 조회할 수 있다")
    void findById_성공() {
        // given
        Point point = Point.create(1L);
        Point saved = repository.save(point);

        // when
        Optional<Point> found = repository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 포인트는 조회되지 않는다")
    void findById_없음() {
        // when
        Optional<Point> found = repository.findById(999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID로 포인트를 조회할 수 있다")
    void findByUserId_성공() {
        // given
        Point point = Point.create(1L);
        repository.save(point);

        // when
        Optional<Point> found = repository.findByUserId(1L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 포인트는 조회되지 않는다")
    void findByUserId_없음() {
        // when
        Optional<Point> found = repository.findByUserId(999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("저장소를 초기화할 수 있다")
    void clear_성공() {
        // given
        repository.save(Point.create(1L));
        repository.save(Point.create(2L));

        // when
        repository.clear();

        // then
        assertThat(repository.findByUserId(1L)).isEmpty();
        assertThat(repository.findByUserId(2L)).isEmpty();
    }
}