package com.personal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioResponse<T> {
    
    private boolean success;
    private T data;
    private String message;
    private String error;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    
    /**
     * Create successful response with data
     */
    public static <T> PortfolioResponse<T> success(T data) {
        return PortfolioResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create successful response with data and message
     */
    public static <T> PortfolioResponse<T> success(T data, String message) {
        return PortfolioResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create successful response with data, message, and metadata
     */
    public static <T> PortfolioResponse<T> success(T data, String message, Map<String, Object> metadata) {
        return PortfolioResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .metadata(metadata)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create error response
     */
    public static <T> PortfolioResponse<T> error(String error) {
        return PortfolioResponse.<T>builder()
                .success(false)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create error response with metadata
     */
    public static <T> PortfolioResponse<T> error(String error, Map<String, Object> metadata) {
        return PortfolioResponse.<T>builder()
                .success(false)
                .error(error)
                .metadata(metadata)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create validation error response
     */
    public static <T> PortfolioResponse<T> validationError(String error) {
        return PortfolioResponse.<T>builder()
                .success(false)
                .error("Validation error: " + error)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create validation error response with field details
     */
    public static <T> PortfolioResponse<T> validationError(String error, List<String> fieldErrors) {
        Map<String, Object> metadata = Map.of("fieldErrors", fieldErrors);
        return PortfolioResponse.<T>builder()
                .success(false)
                .error("Validation error: " + error)
                .metadata(metadata)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create not found response
     */
    public static <T> PortfolioResponse<T> notFound(String resource) {
        return PortfolioResponse.<T>builder()
                .success(false)
                .error(resource + " not found")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create API error response
     */
    public static <T> PortfolioResponse<T> apiError(String service) {
        return PortfolioResponse.<T>builder()
                .success(false)
                .error(service + " API is currently unavailable")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create rate limit error response
     */
    public static <T> PortfolioResponse<T> rateLimitError() {
        return PortfolioResponse.<T>builder()
                .success(false)
                .error("Rate limit exceeded. Please try again later.")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create network error response
     */
    public static <T> PortfolioResponse<T> networkError() {
        return PortfolioResponse.<T>builder()
                .success(false)
                .error("Network error. Please check your connection and try again.")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create timeout error response
     */
    public static <T> PortfolioResponse<T> timeoutError() {
        return PortfolioResponse.<T>builder()
                .success(false)
                .error("Request timed out. Please try again.")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create duplicate resource error response
     */
    public static <T> PortfolioResponse<T> duplicateError(String resource) {
        return PortfolioResponse.<T>builder()
                .success(false)
                .error(resource + " already exists")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create limit exceeded error response
     */
    public static <T> PortfolioResponse<T> limitExceededError(String limit) {
        return PortfolioResponse.<T>builder()
                .success(false)
                .error("Limit exceeded: " + limit)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Check if response has data
     */
    public boolean hasData() {
        return success && data != null;
    }
    
    /**
     * Check if response has metadata
     */
    public boolean hasMetadata() {
        return metadata != null && !metadata.isEmpty();
    }
    
    /**
     * Get metadata value by key
     */
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * Add metadata entry
     */
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
        }
        metadata.put(key, value);
    }
    
    @Override
    public String toString() {
        if (success) {
            return String.format("PortfolioResponse{success=true, message='%s', timestamp=%s}", 
                    message, timestamp);
        } else {
            return String.format("PortfolioResponse{success=false, error='%s', timestamp=%s}", 
                    error, timestamp);
        }
    }
}