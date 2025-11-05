package com.hhplus.hhplus_ecommerce.product.domain;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

class ProductTest {

    @Test
    @DisplayName("상품을 생성할 수 있다")
    void create_성공() {
        // when
        Product product = Product.create("노트북", "고성능", 1000000, 10, "전자제품");

        // then
        assertAll(
                () -> assertThat(product.getName()).isEqualTo("노트북"),
                () -> assertThat(product.getDescription()).isEqualTo("고성능"),
                () -> assertThat(product.getPrice()).isEqualTo(1000000),
                () -> assertThat(product.getStock()).isEqualTo(10),
                () -> assertThat(product.getCategory()).isEqualTo("전자제품"),
                () -> assertThat(product.getCreatedAt()).isNotNull(),
                () -> assertThat(product.getUpdatedAt()).isNotNull()
        );
    }

    @Test
    @DisplayName("재고를 차감할 수 있다")
    void decreaseStock_성공() {
        // given
        Product product = Product.create("노트북", "고성능", 2000000, 10, "전자제품");

        // when
        product.decreaseStock(3);

        // then
        assertThat(product.getStock()).isEqualTo(7);
    }

    @Test
    @DisplayName("재고가 부족하면 예외가 발생한다")
    void decreaseStock_재고부족_예외() {
        // given
        Product product = Product.create("노트북", "고성능", 2000000, 2, "전자제품");

        // when & then
        assertThatThrownBy(() -> product.decreaseStock(3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("재고를 증가시킬 수 있다")
    void increaseStock_성공() {
        // given
        Product product = Product.create("노트북", "고성능", 2000000, 5, "전자제품");

        // when
        product.increaseStock(3);

        // then
        assertThat(product.getStock()).isEqualTo(8);
    }

    @Test
    @DisplayName("상품명이 null이면 생성할 수 없다")
    void create_상품명null_예외() {
        // when & then
        assertThatThrownBy(() -> Product.create(null, "고성능", 1000000, 10, "전자제품"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_INVALID_NAME);
    }

    @Test
    @DisplayName("상품 생성 시 이름이 비어있으면 예외가 발생한다")
    void create_이름없음_예외() {
        // when & then
        assertThatThrownBy(() -> Product.create("", "고성능", 2000000, 10, "전자제품"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_INVALID_NAME);
    }

    @Test
    @DisplayName("상품명이 공백이면 생성할 수 없다")
    void create_상품명공백_예외() {
        // when & then
        assertThatThrownBy(() -> Product.create("   ", "고성능", 1000000, 10, "전자제품"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_INVALID_NAME);
    }

    @Test
    @DisplayName("가격이 null이면 생성할 수 없다")
    void create_가격null_예외() {
        // when & then
        assertThatThrownBy(() -> Product.create("노트북", "고성능", null, 10, "전자제품"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_INVALID_PRICE);
    }

    @Test
    @DisplayName("상품 생성 시 가격이 음수면 예외가 발생한다")
    void create_가격음수_예외() {
        // when & then
        assertThatThrownBy(() -> Product.create("노트북", "고성능", -1000, 10, "전자제품"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_INVALID_PRICE);
    }

    @Test
    @DisplayName("재고가 null이면 생성할 수 없다")
    void create_재고null_예외() {
        // when & then
        assertThatThrownBy(() -> Product.create("노트북", "고성능", 1000000, null, "전자제품"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_INVALID_STOCK);
    }

    @Test
    @DisplayName("상품 생성 시 재고가 음수면 예외가 발생한다")
    void create_재고음수_예외() {
        // when & then
        assertThatThrownBy(() -> Product.create("노트북", "고성능", 2000000, -1, "전자제품"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_INVALID_STOCK);
    }
}