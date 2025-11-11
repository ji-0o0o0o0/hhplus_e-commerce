package com.hhplus.hhplus_ecommerce.cart.application;

import com.hhplus.hhplus_ecommerce.cart.domain.CartItem;
import com.hhplus.hhplus_ecommerce.cart.repository.CartItemRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private Long userId;
    private Long productId;
    private Product product;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        userId = 1L;
        productId = 1L;

        product = Product.builder()
                .id(productId)
                .name("노트북")
                .price(1000000)
                .stock(10)
                .category("전자제품")
                .build();

        cartItem = CartItem.builder()
                .id(1L)
                .userId(userId)
                .productId(productId)
                .quantity(2)
                .build();
    }

    @Test
    @DisplayName("새로운 상품을 장바구니에 추가할 수 있다")
    void addCartItem_새상품_성공() {
        // given
        given(productRepository.findById(productId)).willReturn(Optional.of(product));
        given(cartItemRepository.findByUserIdAndProductId(userId, productId))
                .willReturn(Optional.empty());
        given(cartItemRepository.save(any(CartItem.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        CartItem result = cartService.addCartItem(userId, productId, 3);

        // then
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getUserId()).isEqualTo(userId),
                () -> assertThat(result.getProductId()).isEqualTo(productId),
                () -> assertThat(result.getQuantity()).isEqualTo(3)
        );
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    @DisplayName("이미 장바구니에 있는 상품은 수량이 증가한다")
    void addCartItem_기존상품_수량증가() {
        // given
        given(productRepository.findById(productId)).willReturn(Optional.of(product));
        given(cartItemRepository.findByUserIdAndProductId(userId, productId))
                .willReturn(Optional.of(cartItem));
        given(cartItemRepository.save(any(CartItem.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        CartItem result = cartService.addCartItem(userId, productId, 3);

        // then
        assertThat(result.getQuantity()).isEqualTo(5);  // 2 + 3
        verify(cartItemRepository).save(cartItem);
    }

    @Test
    @DisplayName("존재하지 않는 상품은 장바구니에 추가할 수 없다")
    void addCartItem_상품없음_예외() {
        // given
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> cartService.addCartItem(userId, productId, 1))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("재고가 부족한 상품은 장바구니에 추가할 수 없다")
    void addCartItem_재고부족_예외() {
        // given
        Product lowStockProduct = Product.builder()
                .id(productId)
                .name("노트북")
                .price(1000000)
                .stock(2)
                .category("전자제품")
                .build();

        given(productRepository.findById(productId)).willReturn(Optional.of(lowStockProduct));

        // when & then
        assertThatThrownBy(() -> cartService.addCartItem(userId, productId, 5))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("사용자의 장바구니 목록을 조회할 수 있다")
    void getCartItems_성공() {
        // given
        CartItem item1 = CartItem.builder().id(1L).userId(userId).productId(1L).quantity(2).build();
        CartItem item2 = CartItem.builder().id(2L).userId(userId).productId(2L).quantity(1).build();
        given(cartItemRepository.findByUserId(userId)).willReturn(List.of(item1, item2));

        // when
        List<CartItem> result = cartService.getCartItems(userId);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("장바구니 항목을 삭제할 수 있다")
    void removeCartItem_성공() {
        // given
        Long cartItemId = 1L;
        given(cartItemRepository.findById(cartItemId)).willReturn(Optional.of(cartItem));

        // when
        cartService.removeCartItem(userId, cartItemId);

        // then
        verify(cartItemRepository).delete(cartItemId);
    }

    @Test
    @DisplayName("존재하지 않는 장바구니 항목은 삭제할 수 없다")
    void removeCartItem_항목없음_예외() {
        // given
        Long cartItemId = 999L;
        given(cartItemRepository.findById(cartItemId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> cartService.removeCartItem(userId, cartItemId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사용자의 장바구니 항목은 삭제할 수 없다")
    void removeCartItem_권한없음_예외() {
        // given
        Long cartItemId = 1L;
        Long otherUserId = 999L;
        given(cartItemRepository.findById(cartItemId)).willReturn(Optional.of(cartItem));

        // when & then
        assertThatThrownBy(() -> cartService.removeCartItem(otherUserId, cartItemId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_ACCESS_DENIED);
    }

    @Test
    @DisplayName("장바구니 항목의 수량을 감소시킬 수 있다")
    void decreaseCartItemQuantity_수량감소_성공() {
        // given
        Long cartItemId = 1L;
        given(cartItemRepository.findById(cartItemId)).willReturn(Optional.of(cartItem));
        given(cartItemRepository.save(any(CartItem.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        CartItem result = cartService.decreaseCartItemQuantity(userId, cartItemId, 1);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getQuantity()).isEqualTo(1);  // 2 - 1
        verify(cartItemRepository).save(cartItem);
    }

    @Test
    @DisplayName("수량 감소 시 0 이하가 되면 항목이 삭제된다")
    void decreaseCartItemQuantity_수량0_삭제() {
        // given
        Long cartItemId = 1L;
        given(cartItemRepository.findById(cartItemId)).willReturn(Optional.of(cartItem));

        // when
        CartItem result = cartService.decreaseCartItemQuantity(userId, cartItemId, 5);

        // then
        assertThat(result).isNull();
        verify(cartItemRepository).delete(cartItemId);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("다른 사용자의 장바구니 항목은 수량 감소할 수 없다")
    void decreaseCartItemQuantity_권한없음_예외() {
        // given
        Long cartItemId = 1L;
        Long otherUserId = 999L;
        given(cartItemRepository.findById(cartItemId)).willReturn(Optional.of(cartItem));

        // when & then
        assertThatThrownBy(() -> cartService.decreaseCartItemQuantity(otherUserId, cartItemId, 1))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_ACCESS_DENIED);
    }

    @Test
    @DisplayName("장바구니를 전체 삭제할 수 있다")
    void clearCart_성공() {
        // when
        cartService.clearCart(userId);

        // then
        verify(cartItemRepository).deleteAllByUserId(userId);
    }
}