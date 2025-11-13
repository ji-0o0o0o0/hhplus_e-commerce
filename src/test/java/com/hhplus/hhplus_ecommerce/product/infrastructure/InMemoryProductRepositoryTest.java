package com.hhplus.hhplus_ecommerce.product.infrastructure;

import com.hhplus.hhplus_ecommerce.product.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class InMemoryProductRepositoryTest {

    private InMemoryProductRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProductRepository();
    }

    @Test
    @DisplayName("새로운 상품을 저장할 수 있다")
    void save_신규_성공() {
        // given
        Product product = Product.create("노트북", "고성능", 1000000L, 10, "전자제품");

        // when
        Product saved = repository.save(product);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("노트북");
        assertThat(saved.getPrice()).isEqualTo(1000000);
    }

    @Test
    @DisplayName("기존 상품을 업데이트할 수 있다")
    void save_업데이트_성공() {
        // given
        Product product = Product.create("노트북", "고성능", 1000000L, 10, "전자제품");
        Product saved = repository.save(product);

        // when
        Product updated = Product.builder()
                .id(saved.getId())
                .name("노트북")
                .description("고성능")
                .price(900000L)
                .stock(5)
                .category("전자제품")
                .build();
        Product result = repository.save(updated);

        // then
        assertThat(result.getPrice()).isEqualTo(900000);
        assertThat(result.getStock()).isEqualTo(5);
    }

    @Test
    @DisplayName("ID로 상품을 조회할 수 있다")
    void findById_성공() {
        // given
        Product product = Product.create("노트북", "고성능", 1000000L, 10, "전자제품");
        Product saved = repository.save(product);

        // when
        Optional<Product> found = repository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("노트북");
    }

    @Test
    @DisplayName("존재하지 않는 상품은 조회되지 않는다")
    void findById_없음() {
        // when
        Optional<Product> found = repository.findById(999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("모든 상품을 조회할 수 있다")
    void findAll_성공() {
        // given
        repository.save(Product.create("노트북", "고성능", 1000000L, 10, "전자제품"));
        repository.save(Product.create("마우스", "무선", 50000L, 20, "전자제품"));

        // when
        List<Product> products = repository.findAll();

        // then
        assertThat(products).hasSize(2);
    }

    @Test
    @DisplayName("카테고리로 상품을 조회할 수 있다")
    void findByCategory_성공() {
        // given
        repository.save(Product.create("노트북", "고성능", 1000000L, 10, "전자제품"));
        repository.save(Product.create("마우스", "무선", 50000L, 20, "전자제품"));
        repository.save(Product.create("의자", "편안한", 200000L, 5, "가구"));

        // when
        List<Product> electronics = repository.findByCategory("전자제품");

        // then
        assertThat(electronics).hasSize(2);
    }

    @Test
    @DisplayName("인기 상품을 조회할 수 있다")
    void findTopSellingProducts_성공() {
        // given
        Product p1 = repository.save(Product.create("노트북", "고성능", 1000000L, 10, "전자제품"));
        Product p2 = repository.save(Product.create("마우스", "무선", 50000L, 20, "전자제품"));
        Product p3 = repository.save(Product.create("키보드", "기계식", 100000L, 15, "전자제품"));

        repository.recordSale(p1.getId(), 100);
        repository.recordSale(p2.getId(), 200);
        repository.recordSale(p3.getId(), 50);

        // when
        List<Product> topProducts = repository.findTopSellingProducts(LocalDateTime.now().minusDays(3), 2);

        // then
        assertThat(topProducts).hasSize(2);
        assertThat(topProducts.get(0).getName()).isEqualTo("마우스");  // 200 sales
        assertThat(topProducts.get(1).getName()).isEqualTo("노트북");  // 100 sales
    }

    @Test
    @DisplayName("판매 기록을 누적할 수 있다")
    void recordSale_누적() {
        // given
        Product product = repository.save(Product.create("노트북", "고성능", 1000000L, 10, "전자제품"));

        // when
        repository.recordSale(product.getId(), 10);
        repository.recordSale(product.getId(), 5);

        // then
        List<Product> topProducts = repository.findTopSellingProducts(LocalDateTime.now().minusDays(1), 1);
        assertThat(topProducts).hasSize(1);
    }

    @Test
    @DisplayName("저장소를 초기화할 수 있다")
    void clear_성공() {
        // given
        repository.save(Product.create("노트북", "고성능", 1000000L, 10, "전자제품"));
        repository.save(Product.create("마우스", "무선", 50000L, 20, "전자제품"));

        // when
        repository.clear();

        // then
        assertThat(repository.findAll()).isEmpty();
    }
}