package com.eldercare.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Cấu hình cho Async và Scheduling
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    // Spring Boot sẽ tự động tạo thread pool cho @Async
    // Nếu cần tùy chỉnh thread pool, có thể thêm @Bean TaskExecutor
}
