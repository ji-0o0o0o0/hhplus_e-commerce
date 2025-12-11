package com.hhplus.hhplus_ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리를 위한 설정
 * - @Async 사용 시 ThreadPoolExecutor 설정 필수
 * - 설정하지 않으면 @Async마다 새 스레드 생성 → 비용 증가
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본 스레드 수
        executor.setCorePoolSize(10);

        // 최대 스레드 수
        executor.setMaxPoolSize(50);

        // Queue 크기
        executor.setQueueCapacity(100);

        // 스레드 이름 prefix
        executor.setThreadNamePrefix("async-event-");

        // 모든 작업이 완료될 때까지 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 최대 대기 시간 (초)
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }
}