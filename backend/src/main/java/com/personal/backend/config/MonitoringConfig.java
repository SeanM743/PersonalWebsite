package com.personal.backend.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(MonitoringProperties.class)
public class MonitoringConfig {

    // Note: PrometheusMeterRegistry is auto-configured by Spring Boot when micrometer-registry-prometheus is on classpath

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags("application", "digital-command-center")
                .commonTags("service", "backend")
                .meterFilter(MeterFilter.deny(id -> {
                    String uri = id.getTag("uri");
                    return uri != null && (
                        uri.startsWith("/actuator") ||
                        uri.startsWith("/error") ||
                        uri.contains("favicon")
                    );
                }));
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> configureMetrics() {
        return registry -> {
            // Configure common tags and filters
            registry.config()
                    .commonTags("application", "digital-command-center")
                    .commonTags("service", "backend");
        };
    }
}