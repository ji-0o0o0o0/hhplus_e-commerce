package com.hhplus.hhplus_ecommerce.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class RedisConfig {

    @Value("classpath:redisson-config.yml")
    private Resource redissonConfigFile;

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
    public RedissonClient redissonClient() throws IOException {
        Config config = Config.fromYAML(redissonConfigFile.getInputStream());
        return Redisson.create(config);
    }
}
