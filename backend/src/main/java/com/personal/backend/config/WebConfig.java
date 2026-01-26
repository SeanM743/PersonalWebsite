package com.personal.backend.config;

import com.personal.backend.interceptor.ThrottlingMetricsInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ThrottlingMetricsInterceptor throttlingMetricsInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(throttlingMetricsInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/**", "/error/**");
    }
}