package com.hhplus.hhplus_ecommerce.concurrency;

import com.hhplus.hhplus_ecommerce.common.exception.BusinessException;
import com.hhplus.hhplus_ecommerce.product.application.ProductService;
import com.hhplus.hhplus_ecommerce.product.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 재고 차감 동시성 테스트
 * - 100명이 동시에 재고 50개 상품을 주문하는 시나리오
 * - Race Condition 방지 검증
 */
class ProductStockConcurrencyTest {

    private ProductService productService;
    private InMemoryProductRepository productRepository;
    private com.hhplus.hhplus_ecommerce.common.lock.LockManager lockManager;

    @BeforeEach
    void setUp() {
        productRepository = new InMemoryProductRepository();
        lockManager = new com.hhplus.hhplus_ecommerce.common.lock.LockManager();
        productService = new ProductService(productRepository, lockManager);
    }

    @Test
    @DisplayName("100명이 동시에 재고 50개 상품 구매 시 정확히 50개만 차감되고 50명만 성공한다")
    void concurrentStockDecrease_shouldDecreaseExactly50() throws InterruptedException {
        // Given: 재고 50개 상품 생성
        Product product = Product.create("인기 상품", "재고 50개", 10000L, 50, "전자기기");
        Product savedProduct = productRepository.save(product);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 100명이 동시에 1개씩 구매 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    productService.decreaseStock(savedProduct.getId(), 1);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 정확히 50명만 성공
        assertEquals(50, successCount.get(), "성공 카운트가 50이어야 함");
        assertEquals(50, failCount.get(), "실패 카운트가 50이어야 함");

        // 실제 재고 확인
        Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertEquals(0, updatedProduct.getStock(), "재고가 정확히 0이어야 함");
    }

    @Test
    @DisplayName("1000명이 동시에 재고 100개 상품 구매 시 정확히 100개만 차감된다")
    void concurrentStockDecrease_highLoad_shouldDecreaseExactly100() throws InterruptedException {
        // Given: 재고 100개 상품 생성
        Product product = Product.create("대용량 상품", "재고 100개", 50000L, 100, "가전");
        Product savedProduct = productRepository.save(product);

        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 1000명이 동시에 1개씩 구매 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    productService.decreaseStock(savedProduct.getId(), 1);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 정확히 100명만 성공
        assertEquals(100, successCount.get(), "성공 카운트가 100이어야 함");
        assertEquals(900, failCount.get(), "실패 카운트가 900이어야 함");

        // 실제 재고 확인
        Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertEquals(0, updatedProduct.getStock(), "재고가 정확히 0이어야 함");
    }

    @Test
    @DisplayName("다양한 수량으로 동시 구매 시 재고가 정확히 계산된다")
    void concurrentStockDecrease_variousQuantities_shouldCalculateCorrectly() throws InterruptedException {
        // Given: 재고 100개 상품 생성
        Product product = Product.create("혼합 구매 상품", "재고 100개", 20000L, 100, "의류");
        Product savedProduct = productRepository.save(product);

        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger totalDecreased = new AtomicInteger(0);

        // When: 50명이 동시에 2개씩 구매 시도 (총 100개)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    productService.decreaseStock(savedProduct.getId(), 2);
                    totalDecreased.addAndGet(2);
                } catch (BusinessException e) {
                    // 재고 부족 시 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 총 차감량이 100 이하여야 함
        assertTrue(totalDecreased.get() <= 100, "총 차감량이 100 이하여야 함");

        // 실제 재고 확인
        Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertEquals(100 - totalDecreased.get(), updatedProduct.getStock(),
                "재고가 (100 - 차감량)과 일치해야 함");
    }

    @Test
    @DisplayName("동시 재고 증가 시 정확히 계산된다")
    void concurrentStockIncrease_shouldIncreaseCorrectly() throws InterruptedException {
        // Given: 재고 0개 상품 생성
        Product product = Product.create("재입고 상품", "재고 0개", 30000L, 0, "식품");
        Product savedProduct = productRepository.save(product);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When: 100명이 동시에 1개씩 재고 증가
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    productService.increaseStock(savedProduct.getId(), 1);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 재고가 정확히 100이어야 함
        Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertEquals(100, updatedProduct.getStock(), "재고가 정확히 100이어야 함");
    }

    @Test
    @DisplayName("동시 재고 차감과 증가가 혼합되어도 정확히 계산된다")
    void concurrentStockMixed_shouldCalculateCorrectly() throws InterruptedException {
        // Given: 재고 50개 상품 생성
        Product product = Product.create("혼합 작업 상품", "재고 50개", 15000L, 50, "도서");
        Product savedProduct = productRepository.save(product);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger decreaseSuccessCount = new AtomicInteger(0);
        AtomicInteger increaseSuccessCount = new AtomicInteger(0);

        // When: 50명은 차감, 50명은 증가 시도
        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            executorService.submit(() -> {
                try {
                    if (finalI < 50) {
                        // 차감
                        productService.decreaseStock(savedProduct.getId(), 1);
                        decreaseSuccessCount.incrementAndGet();
                    } else {
                        // 증가
                        productService.increaseStock(savedProduct.getId(), 1);
                        increaseSuccessCount.incrementAndGet();
                    }
                } catch (BusinessException e) {
                    // 재고 부족 시 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 재고가 정확히 계산되어야 함
        Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        int expectedStock = 50 - decreaseSuccessCount.get() + increaseSuccessCount.get();
        assertEquals(expectedStock, updatedProduct.getStock(),
                "재고가 (50 - 차감 성공 + 증가 성공)과 일치해야 함");
    }
}
