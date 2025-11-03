package com.hhplus.hhplus_ecommerce.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("항해플러스 E-Commerce API")
                        .description("항해플러스 이커머스 서비스 API 문서")
                        .version("1.0.0"));
    }
}
