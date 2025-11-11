package com.hhplus.hhplus_ecommerce.product.domain;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private Long id;
    private String name;
    private String description;
    private Integer price;
    private Integer stock;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Product create(String name, String description,
                                 Integer price, Integer stock, String category) {
        validateCreate(name, price, stock);
        return Product.builder()
                .name(name)
                .description(description)
                .price(price)
                .stock(stock)
                .category(category)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    //비즈니스 로직
    public void decreaseStock(Integer quantity) {
        if(this.stock < quantity ) {
            throw new BusinessException(ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
        }
        this.stock -= quantity;
        this.updatedAt = LocalDateTime.now();
    }
    public void increaseStock(Integer quantity) {
        this.stock += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateCreate(String name, Integer price, Integer stock) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_NAME);
        }
        if (price == null || price < 0) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_PRICE);
        }
        if (stock == null || stock < 0) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_STOCK);
        }
    }
}
