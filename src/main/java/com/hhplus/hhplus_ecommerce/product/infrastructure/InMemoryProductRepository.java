package com.hhplus.hhplus_ecommerce.product.infrastructure;

import com.hhplus.hhplus_ecommerce.product.domain.Product;
import com.hhplus.hhplus_ecommerce.product.repository.ProductRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final Map<Long, Product> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    // 판매 기록 (상품ID -> 판매량)
    private final Map<Long, Integer> salesRecord = new ConcurrentHashMap<>();

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            // 새로운 상품 저장
            Product newProduct = Product.builder()
                    .id(idGenerator.getAndIncrement())
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .stock(product.getStock())
                    .category(product.getCategory())
                    .createdAt(product.getCreatedAt())
                    .updatedAt(product.getUpdatedAt())
                    .build();
            store.put(newProduct.getId(), newProduct);
            return newProduct;
        } else {
            // 기존 상품 업데이트
            store.put(product.getId(), product);
            return product;
        }
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Product> findByCategory(String category) {
        return store.values().stream()
                .filter(p -> category.equals(p.getCategory()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findTopSellingProducts(LocalDateTime startDate, int limit) {
        return salesRecord.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> store.get(entry.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // 판매 기록 추가 (테스트/초기화 용도)
    public void recordSale(Long productId, Integer quantity) {
        salesRecord.merge(productId, quantity, Integer::sum);
    }

    // 테스트용: 전체 데이터 삭제
    public void clear() {
        store.clear();
        salesRecord.clear();
        idGenerator.set(1);
    }
}