package com.personal.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentResponse<T> {
    
    private T data;
    
    private boolean success;
    
    private String message;
    
    private Map<String, Object> metadata;
    
    private Instant timestamp;
    
    public static <T> ContentResponse<T> success(T data) {
        return ContentResponse.<T>builder()
                .data(data)
                .success(true)
                .timestamp(Instant.now())
                .build();
    }
    
    public static <T> ContentResponse<T> success(T data, String message) {
        return ContentResponse.<T>builder()
                .data(data)
                .success(true)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
    
    public static <T> ContentResponse<T> error(String message) {
        return ContentResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
    
    public static <T> ContentResponse<T> error(String message, Map<String, Object> metadata) {
        return ContentResponse.<T>builder()
                .success(false)
                .message(message)
                .metadata(metadata)
                .timestamp(Instant.now())
                .build();
    }
}