package com.personal.backend;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableRetry
@EnableCaching
@Slf4j
public class BackendApplication {

    private final Environment environment;

    public BackendApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = environment.getProperty("server.port", "8080");
        log.info("🚀 Personal Dashboard Backend is ready!");
        log.info("🌐 Server running on: http://localhost:{}", port);
        log.info("📊 Portfolio API: http://localhost:{}/api/portfolio", port);
        log.info("💬 Chat API: http://localhost:{}/api/chat", port);
        log.info("📅 Calendar API: http://localhost:{}/api/calendar", port);
        log.info("📝 Content API: http://localhost:{}/api/content", port);
        log.info("📰 News API: http://localhost:{}/api/news", port);
        log.info("🔐 Auth API: http://localhost:{}/api/auth", port);
        log.info("✅ Application startup completed successfully!");
    }
}
