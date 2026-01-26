package com.personal.backend.interceptor;

import com.personal.backend.service.MetricsService;
import com.personal.backend.service.PerformanceMetricsService;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class ThrottlingMetricsInterceptor implements HandlerInterceptor {

    private static final String REQUEST_START_TIME = "request_start_time";
    private static final String TIMER_SAMPLE = "timer_sample";
    private static final String THROTTLING_START_TIME = "throttling_start_time";
    
    private final MetricsService metricsService;
    private final PerformanceMetricsService performanceMetricsService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Record request start time
        request.setAttribute(REQUEST_START_TIME, Instant.now());
        
        // Start metrics timer
        Timer.Sample sample = metricsService.startRequestTimer();
        if (sample != null) {
            request.setAttribute(TIMER_SAMPLE, sample);
        }
        
        // Record throttling decision start time
        request.setAttribute(THROTTLING_START_TIME, Instant.now());
        
        // Check if request should be throttled (simplified logic)
        boolean shouldThrottle = shouldThrottleRequest(request);
        
        if (shouldThrottle) {
            // Record throttling decision time
            Instant throttlingStart = (Instant) request.getAttribute(THROTTLING_START_TIME);
            Duration throttlingDuration = Duration.between(throttlingStart, Instant.now());
            
            String endpoint = getEndpointName(request);
            metricsService.recordThrottlingDecision(throttlingDuration, endpoint, true);
            metricsService.recordThrottledRequest(endpoint, "rate_limit");
            
            // Set throttling response
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setHeader("Retry-After", "60");
            
            log.warn("Request throttled for endpoint: {} from IP: {}", endpoint, getClientIpAddress(request));
            return false; // Block the request
        } else {
            // Record successful throttling decision
            Instant throttlingStart = (Instant) request.getAttribute(THROTTLING_START_TIME);
            Duration throttlingDuration = Duration.between(throttlingStart, Instant.now());
            String endpoint = getEndpointName(request);
            metricsService.recordThrottlingDecision(throttlingDuration, endpoint, false);
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        // Calculate request duration
        Instant startTime = (Instant) request.getAttribute(REQUEST_START_TIME);
        Duration requestDuration = Duration.ZERO;
        if (startTime != null) {
            requestDuration = Duration.between(startTime, Instant.now());
        }
        
        // Stop metrics timer
        Timer.Sample sample = (Timer.Sample) request.getAttribute(TIMER_SAMPLE);
        if (sample != null) {
            String endpoint = getEndpointName(request);
            String method = request.getMethod();
            String status = String.valueOf(response.getStatus());
            
            metricsService.stopRequestTimer(sample, endpoint, method, status);
        }
        
        // Record performance metrics
        String endpoint = getEndpointName(request);
        String method = request.getMethod();
        int statusCode = response.getStatus();
        
        performanceMetricsService.recordHttpRequest(endpoint, method, statusCode, requestDuration);
        
        // Log request completion
        if (startTime != null) {
            log.debug("Request completed: {} {} in {}ms with status {}", 
                    method, 
                    endpoint, 
                    requestDuration.toMillis(), 
                    statusCode);
        }
    }

    /**
     * Simple throttling logic - can be enhanced with more sophisticated algorithms
     */
    private boolean shouldThrottleRequest(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        String endpoint = getEndpointName(request);
        
        // Skip throttling for actuator endpoints
        if (endpoint.startsWith("/actuator")) {
            return false;
        }
        
        // Simple rate limiting based on current throttling rate
        double currentThrottlingRate = metricsService.getCurrentThrottlingRate();
        
        // If current throttling rate is high, be more aggressive
        if (currentThrottlingRate > 10.0) {
            // Throttle 20% of requests when throttling rate is high
            return Math.random() < 0.2;
        } else if (currentThrottlingRate > 5.0) {
            // Throttle 10% of requests when throttling rate is moderate
            return Math.random() < 0.1;
        }
        
        // Basic IP-based throttling (simplified)
        // In production, use Redis or similar for distributed rate limiting
        return isIpThrottled(clientIp, endpoint);
    }

    /**
     * Simple IP-based throttling check
     * In production, this should use a proper rate limiting algorithm with Redis
     */
    private boolean isIpThrottled(String clientIp, String endpoint) {
        // For demo purposes, throttle requests from localhost if too frequent
        // This is a simplified implementation
        return false; // Disabled for now
    }

    /**
     * Extract endpoint name from request
     */
    private String getEndpointName(HttpServletRequest request) {
        String uri = request.getRequestURI();
        
        // Normalize endpoint names for better metrics grouping
        if (uri.startsWith("/api/")) {
            // Remove query parameters
            int queryIndex = uri.indexOf('?');
            if (queryIndex > 0) {
                uri = uri.substring(0, queryIndex);
            }
            
            // Replace IDs with placeholders for better grouping
            uri = uri.replaceAll("/\\d+", "/{id}");
            uri = uri.replaceAll("/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}", "/{uuid}");
        }
        
        return uri;
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}