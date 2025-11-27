package com.hhplus.hhplus_ecommerce.integration;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestRedisConfig {

    @Bean
    @Primary
    public RedissonClient testRedissonClient(
            @Value("${spring.data.redis.host}") String redisHost,
            @Value("${spring.data.redis.port}") String redisPort) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setConnectionPoolSize(64)
                .setConnectionMinimumIdleSize(10)
                .setConnectTimeout(10000)
                .setTimeout(3000);
        return Redisson.create(config);
    }
}