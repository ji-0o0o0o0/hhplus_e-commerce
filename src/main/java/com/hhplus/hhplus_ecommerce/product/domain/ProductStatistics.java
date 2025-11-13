package com.hhplus.hhplus_ecommerce.product.domain;

import com.hhplus.hhplus_ecommerce.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "product_statistics")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatistics extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private LocalDate statsDate;

    @Column(nullable = false)
    private Integer salesCount;

    @Column(nullable = false)
    private Long revenue;

}
