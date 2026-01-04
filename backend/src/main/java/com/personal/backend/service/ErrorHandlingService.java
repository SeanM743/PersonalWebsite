package com.personal.backend.service;

import com.personal.backend.dto.ContentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class ErrorHandlingService {
    
    public <T> ContentResponse<T> handleServiceError(Exception e, String operation) {
        log.error("Error in operation: {}", operation, e);
        
        if (e instanceof IllegalArgumentException) {
            return ContentResponse.error("Invalid input: " + e.getMessage());
        }
        
        if (e instanceof DataIntegrityViolationException) {
            return ContentResponse.error("Data constraint violation: " + extractConstraintMessage((DataIntegrityViolationException) e));
        }
        
        if (e instanceof DataAccessException) {
            return ContentResponse.error("Database error occurred. Please try again later.");
        }
        
        if (e instanceof RestClientException) {
            return handleExternalApiError(e);
        }
        
        if (e instanceof TimeoutException) {
            return ContentResponse.error("Operation timed out. Please try again.");
        }
        
        // Generic error for unexpected exceptions
        return ContentResponse.error("An unexpected error occurred: " + e.getMessage());
    }
    
    public <T> ContentResponse<T> handleExternalApiError(Exception e) {
        log.warn("External API error: {}", e.getMessage());
        
        if (e instanceof ResourceAccessException) {
            return ContentResponse.error("External service is temporarily unavailable. Using fallback data.");
        }
        
        if (e instanceof RestClientException) {
            String message = e.getMessage();
            if (message != null && message.contains("429")) {
                return ContentResponse.error("Rate limit exceeded for external API. Please try again later.");
            }
            if (message != null && message.contains("404")) {
                return ContentResponse.error("Requested resource not found in external API.");
            }
            if (message != null && message.contains("401")) {
                return ContentResponse.error("Authentication failed with external API.");
            }
        }
        
        return ContentResponse.error("External API error: " + e.getMessage());
    }
    
    public ContentResponse<Void> handleValidationErrors(Map<String, String> errors) {
        StringBuilder errorMessage = new StringBuilder("Validation failed: ");
        errors.forEach((field, message) -> 
            errorMessage.append(field).append(" - ").append(message).append("; "));
        
        // Convert Map<String, String> to Map<String, Object>
        Map<String, Object> errorMetadata = new HashMap<>(errors);
        return ContentResponse.error(errorMessage.toString(), errorMetadata);
    }
    
    public String getUserFriendlyMessage(Exception e) {
        if (e instanceof IllegalArgumentException) {
            return "Please check your input and try again.";
        }
        
        if (e instanceof DataIntegrityViolationException) {
            return "This operation conflicts with existing data. Please check for duplicates.";
        }
        
        if (e instanceof DataAccessException) {
            return "We're experiencing database issues. Please try again in a few moments.";
        }
        
        if (e instanceof RestClientException) {
            return "External service is temporarily unavailable. Some features may be limited.";
        }
        
        if (e instanceof TimeoutException) {
            return "The operation is taking longer than expected. Please try again.";
        }
        
        return "Something went wrong. Our team has been notified.";
    }
    
    private String extractConstraintMessage(DataIntegrityViolationException e) {
        String message = e.getMessage();
        if (message == null) {
            return "Data constraint violation";
        }
        
        // Extract meaningful constraint information
        if (message.contains("unique")) {
            return "A record with this information already exists";
        }
        if (message.contains("foreign key")) {
            return "Referenced data does not exist";
        }
        if (message.contains("not null")) {
            return "Required field is missing";
        }
        
        return "Data constraint violation";
    }
    
    public void logOperationMetrics(String operation, long duration, boolean success) {
        if (success) {
            log.info("Operation {} completed successfully in {}ms", operation, duration);
        } else {
            log.warn("Operation {} failed after {}ms", operation, duration);
        }
    }
    
    public void logPerformanceWarning(String operation, long duration, long threshold) {
        if (duration > threshold) {
            log.warn("Performance warning: {} took {}ms (threshold: {}ms)", operation, duration, threshold);
        }
    }
}