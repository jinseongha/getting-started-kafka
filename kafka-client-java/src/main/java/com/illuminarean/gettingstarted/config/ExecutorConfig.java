package com.illuminarean.gettingstarted.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutorConfig {
    @Bean
    public TaskExecutor executor() {
        final var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(30);
        executor.afterPropertiesSet();
        return executor;
    }
}
