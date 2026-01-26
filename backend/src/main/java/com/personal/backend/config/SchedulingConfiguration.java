package com.personal.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration for scheduled tasks and market data updates
 * Scheduling is disabled by default to prevent automatic API calls
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(value = "portfolio.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class SchedulingConfiguration {
    
    @Value("${portfolio.scheduler.pool.size:5}")
    private int schedulerPoolSize;
    
    @Value("${portfolio.scheduler.thread.name.prefix:market-data-}")
    private String threadNamePrefix;
    
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(schedulerPoolSize);
        scheduler.setThreadNamePrefix(threadNamePrefix);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        return scheduler;
    }
}