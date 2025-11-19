package com.hhplus.hhplus_ecommerce.product.domain;

import com.hhplus.hhplus_ecommerce.common.BaseTimeEntity;
import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Integer stock;

    @Column(length = 100)
    private String category;

    @Version
    private Long version;


    public static Product create(String name, String description,
                                 Long price, Integer stock, String category) {
        validateCreate(name, price, stock);
        return Product.builder()
                .name(name)
                .description(description)
                .price(price)
                .stock(stock)
                .category(category)
                .build();
    }

    public void decreaseStock(Integer quantity) {
        if(this.stock < quantity ) {
            throw new BusinessException(ErrorCode.PRODUCT_INSUFFICIENT_STOCK);
        }
        this.stock -= quantity;
    }
    public void increaseStock(Integer quantity) {
        this.stock += quantity;
    }
    public boolean hasSufficientStock(Integer quantity) {
        return this.stock >= quantity;
    }

    private static void validateCreate(String name, Long price, Integer stock) {
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
