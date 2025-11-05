package com.hhplus.hhplus_ecommerce.product.application;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private Long productId;

    @BeforeEach
    void setUp() {
        productId = 1L;
        product = Product.builder()
                .id(productId)
                .name("노트북")
                .description("고성능 노트북")
                .price(1000000)
                .stock(10)
                .category("전자제품")
                .build();
    }

    @Test
    @DisplayName("상품을 조회할 수 있다")
    void getProduct_성공() {
        // given
        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when
        Product result = productService.getProduct(productId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(productId);
        assertThat(result.getName()).isEqualTo("노트북");
    }

    @Test
    @DisplayName("존재하지 않는 상품을 조회하면 예외가 발생한다")
    void getProduct_상품없음_예외() {
        // given
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(productId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("모든 상품 목록을 조회할 수 있다")
    void getProducts_성공() {
        // given
        Product product2 = Product.builder()
                .id(2L)
                .name("마우스")
                .price(50000)
                .stock(20)
                .category("전자제품")
                .build();
        given(productRepository.findAll()).willReturn(List.of(product, product2));

        // when
        List<Product> result = productService.getProducts();

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("카테고리별 상품 목록을 조회할 수 있다")
    void getProductsByCategory_성공() {
        // given
        String category = "전자제품";
        given(productRepository.findByCategory(category)).willReturn(List.of(product));

        // when
        List<Product> result = productService.getProductsByCategory(category);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo(category);
    }

    @Test
    @DisplayName("인기 상품 목록을 조회할 수 있다")
    void getTopProducts_성공() {
        // given
        given(productRepository.findTopSellingProducts(any(LocalDateTime.class), eq(5)))
                .willReturn(List.of(product));

        // when
        List<Product> result = productService.getTopProducts();

        // then
        assertThat(result).hasSize(1);
        verify(productRepository).findTopSellingProducts(any(LocalDateTime.class), eq(5));
    }
}