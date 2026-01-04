package com.personal.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive error handling and retry mechanisms for Finnhub API integration
 */
@Service
@Slf4j
public class FinnhubErrorHandler {
    
    @Value("${portfolio.finnhub.retry.max.attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${portfolio.finnhub.retry.initial.delay:1000}")
    private long initialRetryDelayMs;
    
    @Value("${portfolio.finnhub.retry.max.delay:10000}")
    private long maxRetryDelayMs;
    
    @Value("${portfolio.finnhub.circuit.breaker.failure.threshold:5}")
    private int circuitBreakerFailureThreshold;
    
    @Value("${portfolio.finnhub.circuit.breaker.timeout:60000}")
    private long circuitBreakerTimeoutMs;
    
    // Circuit breaker state
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private volatile boolean circuitOpen = false;
    
    /**
     * Create retry specification with exponential backoff
     */
    public Retry createRetrySpec() {
        return Retry.backoff(maxRetryAttempts, Duration.ofMillis(initialRetryDelayMs))
                .maxBackoff(Duration.ofMillis(maxRetryDelayMs))
                .jitter(0.1) // Add 10% jitter to prevent thundering herd
                .filter(this::isRetryableException)
                .doBeforeRetry(retrySignal -> {
                    log.warn("Retrying Finnhub API request (attempt {}/{}): {}", 
                            retrySignal.totalRetries() + 1, maxRetryAttempts, 
                            retrySignal.failure().getMessage());
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    recordFailure();
                    return new FinnhubApiException("Max retry attempts exceeded: " + 
                            retrySignal.failure().getMessage(), retrySignal.failure());
                });
    }
    
    /**
     * Handle API errors with appropriate fallback strategies
     */
    public <T> Mono<T> handleError(Throwable throwable, String operation, Mono<T> fallback) {
        if (throwable instanceof WebClientResponseException webEx) {
            return handleWebClientError(webEx, operation, fallback);
        } else if (throwable instanceof FinnhubApiException) {
            return handleFinnhubApiError((FinnhubApiException) throwable, operation, fallback);
        } else {
            return handleGenericError(throwable, operation, fallback);
        }
    }
    
    /**
     * Check if circuit breaker is open
     */
    public boolean isCircuitOpen() {
        if (circuitOpen) {
            long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
            if (timeSinceLastFailure > circuitBreakerTimeoutMs) {
                // Try to close circuit
                circuitOpen = false;
                consecutiveFailures.set(0);
                log.info("Circuit breaker closed - attempting to resume Finnhub API calls");
                return false;
            }
            return true;
        }
        return false;
    }
    
    /**
     * Check circuit breaker before making API calls
     */
    public <T> Mono<T> checkCircuitBreaker(Mono<T> operation) {
        if (isCircuitOpen()) {
            log.warn("Circuit breaker is open - skipping Finnhub API call");
            return Mono.error(new FinnhubApiException("Circuit breaker is open"));
        }
        
        return operation
                .doOnSuccess(result -> recordSuccess())
                .doOnError(throwable -> recordFailure());
    }
    
    /**
     * Determine if an exception is retryable
     */
    public boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException webEx) {
            HttpStatus status = (HttpStatus) webEx.getStatusCode();
            
            // Retry on server errors, rate limits, and timeouts
            if (status.is5xxServerError()) {
                log.debug("Retryable server error: {}", status);
                return true;
            }
            
            if (status == HttpStatus.TOO_MANY_REQUESTS) {
                log.debug("Rate limit exceeded - will retry with backoff");
                return true;
            }
            
            if (status == HttpStatus.REQUEST_TIMEOUT) {
                log.debug("Request timeout - will retry");
                return true;
            }
            
            // Don't retry on client errors (4xx except 429)
            if (status.is4xxClientError()) {
                log.debug("Non-retryable client error: {}", status);
                return false;
            }
        }
        
        // Retry on network-related exceptions
        if (throwable instanceof java.net.ConnectException) {
            log.debug("Connection error - will retry");
            return true;
        }
        
        if (throwable instanceof java.util.concurrent.TimeoutException) {
            log.debug("Timeout error - will retry");
            return true;
        }
        
        if (throwable instanceof java.net.SocketTimeoutException) {
            log.debug("Socket timeout - will retry");
            return true;
        }
        
        // Don't retry on other exceptions
        log.debug("Non-retryable exception: {}", throwable.getClass().getSimpleName());
        return false;
    }
    
    /**
     * Handle WebClient response exceptions
     */
    private <T> Mono<T> handleWebClientError(WebClientResponseException webEx, String operation, Mono<T> fallback) {
        HttpStatus status = (HttpStatus) webEx.getStatusCode();
        String responseBody = webEx.getResponseBodyAsString();
        
        switch (status) {
            case UNAUTHORIZED:
                log.error("Finnhub API authentication failed for {}: Invalid API key", operation);
                return Mono.error(new FinnhubApiException("Invalid API key", webEx));
                
            case FORBIDDEN:
                log.error("Finnhub API access forbidden for {}: Insufficient permissions", operation);
                return Mono.error(new FinnhubApiException("API access forbidden", webEx));
                
            case TOO_MANY_REQUESTS:
                log.warn("Finnhub API rate limit exceeded for {}: {}", operation, responseBody);
                return fallback != null ? fallback : 
                        Mono.error(new FinnhubApiException("Rate limit exceeded", webEx));
                
            case NOT_FOUND:
                log.warn("Finnhub API resource not found for {}: {}", operation, responseBody);
                return fallback != null ? fallback : 
                        Mono.error(new FinnhubApiException("Resource not found", webEx));
                
            case INTERNAL_SERVER_ERROR:
            case BAD_GATEWAY:
            case SERVICE_UNAVAILABLE:
            case GATEWAY_TIMEOUT:
                log.error("Finnhub API server error for {}: {} - {}", operation, status, responseBody);
                return fallback != null ? fallback : 
                        Mono.error(new FinnhubApiException("Server error: " + status, webEx));
                
            default:
                log.error("Unexpected Finnhub API error for {}: {} - {}", operation, status, responseBody);
                return fallback != null ? fallback : 
                        Mono.error(new FinnhubApiException("Unexpected API error: " + status, webEx));
        }
    }
    
    /**
     * Handle Finnhub-specific API exceptions
     */
    private <T> Mono<T> handleFinnhubApiError(FinnhubApiException apiEx, String operation, Mono<T> fallback) {
        log.error("Finnhub API error for {}: {}", operation, apiEx.getMessage());
        return fallback != null ? fallback : Mono.error(apiEx);
    }
    
    /**
     * Handle generic errors
     */
    private <T> Mono<T> handleGenericError(Throwable throwable, String operation, Mono<T> fallback) {
        log.error("Unexpected error during {}: {}", operation, throwable.getMessage(), throwable);
        return fallback != null ? fallback : 
                Mono.error(new FinnhubApiException("Unexpected error: " + throwable.getMessage(), throwable));
    }
    
    /**
     * Record successful API call
     */
    private void recordSuccess() {
        if (consecutiveFailures.get() > 0) {
            log.debug("Finnhub API call succeeded - resetting failure count");
            consecutiveFailures.set(0);
        }
    }
    
    /**
     * Record failed API call and check circuit breaker
     */
    private void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        log.warn("Finnhub API failure recorded - consecutive failures: {}", failures);
        
        if (failures >= circuitBreakerFailureThreshold && !circuitOpen) {
            circuitOpen = true;
            log.error("Circuit breaker opened after {} consecutive failures - Finnhub API calls suspended for {} ms", 
                    failures, circuitBreakerTimeoutMs);
        }
    }
    
    /**
     * Get current error statistics
     */
    public ErrorStatistics getErrorStatistics() {
        return new ErrorStatistics(
                consecutiveFailures.get(),
                lastFailureTime.get(),
                circuitOpen,
                circuitBreakerFailureThreshold,
                circuitBreakerTimeoutMs
        );
    }
    
    /**
     * Reset circuit breaker manually (for testing or admin operations)
     */
    public void resetCircuitBreaker() {
        circuitOpen = false;
        consecutiveFailures.set(0);
        lastFailureTime.set(0);
        log.info("Circuit breaker manually reset");
    }
    
    /**
     * Error statistics data class
     */
    public record ErrorStatistics(
            int consecutiveFailures,
            long lastFailureTime,
            boolean circuitOpen,
            int failureThreshold,
            long timeoutMs
    ) {}
    
    /**
     * Custom exception for Finnhub API errors
     */
    public static class FinnhubApiException extends RuntimeException {
        public FinnhubApiException(String message) {
            super(message);
        }
        
        public FinnhubApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}