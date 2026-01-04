package com.personal.backend.service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.personal.backend.dto.CalendarResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GoogleCalendarErrorHandler {
    
    private static final Map<Integer, String> HTTP_ERROR_MESSAGES = Map.of(
            400, "Bad request - invalid parameters",
            401, "Unauthorized - authentication required",
            403, "Forbidden - insufficient permissions",
            404, "Not found - resource does not exist",
            409, "Conflict - resource already exists or version mismatch",
            429, "Rate limit exceeded - too many requests",
            500, "Internal server error - Google Calendar service issue",
            503, "Service unavailable - Google Calendar temporarily down"
    );
    
    /**
     * Handle Google Calendar API errors with appropriate response mapping
     */
    public <T> CalendarResponse<T> handleApiError(Exception e, String operation) {
        log.error("Google Calendar API error during {}: {}", operation, e.getMessage(), e);
        
        if (e instanceof GoogleJsonResponseException googleError) {
            return handleGoogleJsonError(googleError, operation);
        } else if (e instanceof IOException ioError) {
            return handleIOError(ioError, operation);
        } else if (e instanceof GeneralSecurityException securityError) {
            return handleSecurityError(securityError, operation);
        } else {
            return handleGenericError(e, operation);
        }
    }
    
    /**
     * Handle Google JSON response exceptions with specific error codes
     */
    private <T> CalendarResponse<T> handleGoogleJsonError(GoogleJsonResponseException e, String operation) {
        int statusCode = e.getStatusCode();
        String errorMessage = e.getDetails() != null ? e.getDetails().getMessage() : e.getMessage();
        
        log.warn("Google Calendar API returned status {}: {} during {}", statusCode, errorMessage, operation);
        
        return switch (statusCode) {
            case 400 -> CalendarResponse.validationError("Invalid request: " + errorMessage);
            case 401 -> CalendarResponse.authenticationError();
            case 403 -> {
                if (errorMessage.toLowerCase().contains("rate") || errorMessage.toLowerCase().contains("quota")) {
                    yield CalendarResponse.rateLimitError();
                } else {
                    yield CalendarResponse.permissionError();
                }
            }
            case 404 -> CalendarResponse.eventNotFound();
            case 409 -> CalendarResponse.conflictError("Resource conflict: " + errorMessage);
            case 429 -> CalendarResponse.rateLimitError();
            case 500, 502, 503, 504 -> CalendarResponse.serviceUnavailableError();
            default -> CalendarResponse.error(HTTP_ERROR_MESSAGES.getOrDefault(statusCode, 
                    "Google Calendar API error (HTTP " + statusCode + "): " + errorMessage));
        };
    }
    
    /**
     * Handle IO exceptions (network, timeout, etc.)
     */
    private <T> CalendarResponse<T> handleIOError(IOException e, String operation) {
        String message = e.getMessage().toLowerCase();
        
        if (e instanceof SocketTimeoutException) {
            log.warn("Timeout during Google Calendar {}: {}", operation, e.getMessage());
            return CalendarResponse.timeoutError();
        } else if (e instanceof UnknownHostException) {
            log.warn("Network connectivity issue during Google Calendar {}: {}", operation, e.getMessage());
            return CalendarResponse.networkError();
        } else if (message.contains("connection") || message.contains("network")) {
            log.warn("Network error during Google Calendar {}: {}", operation, e.getMessage());
            return CalendarResponse.networkError();
        } else if (message.contains("rate limit") || message.contains("quota")) {
            log.warn("Rate limit hit during Google Calendar {}: {}", operation, e.getMessage());
            return CalendarResponse.rateLimitError();
        } else if (message.contains("unauthorized") || message.contains("authentication")) {
            log.warn("Authentication error during Google Calendar {}: {}", operation, e.getMessage());
            return CalendarResponse.authenticationError();
        } else if (message.contains("forbidden") || message.contains("permission")) {
            log.warn("Permission error during Google Calendar {}: {}", operation, e.getMessage());
            return CalendarResponse.permissionError();
        } else {
            log.error("IO error during Google Calendar {}: {}", operation, e.getMessage());
            return CalendarResponse.error("Network or communication error: " + e.getMessage());
        }
    }
    
    /**
     * Handle security exceptions
     */
    private <T> CalendarResponse<T> handleSecurityError(GeneralSecurityException e, String operation) {
        log.error("Security error during Google Calendar {}: {}", operation, e.getMessage());
        
        String message = e.getMessage().toLowerCase();
        if (message.contains("credential") || message.contains("authentication")) {
            return CalendarResponse.authenticationError();
        } else {
            return CalendarResponse.error("Security error: " + e.getMessage());
        }
    }
    
    /**
     * Handle generic exceptions
     */
    private <T> CalendarResponse<T> handleGenericError(Exception e, String operation) {
        log.error("Unexpected error during Google Calendar {}: {}", operation, e.getMessage(), e);
        return CalendarResponse.error("Unexpected error during " + operation + ": " + e.getMessage());
    }
    
    /**
     * Calculate exponential backoff delay for retry operations
     */
    public long calculateBackoffDelay(int attemptNumber, long baseDelayMs) {
        return Math.min(baseDelayMs * (long) Math.pow(2, attemptNumber - 1), TimeUnit.MINUTES.toMillis(5));
    }
    
    /**
     * Determine if an exception is retryable
     */
    public boolean isRetryableError(Exception e) {
        if (e instanceof GoogleJsonResponseException googleError) {
            int statusCode = googleError.getStatusCode();
            // Retry on server errors and rate limits, but not on client errors
            return statusCode >= 500 || statusCode == 429 || statusCode == 408;
        } else if (e instanceof IOException) {
            String message = e.getMessage().toLowerCase();
            // Retry on network issues and timeouts
            return e instanceof SocketTimeoutException || 
                   message.contains("connection") || 
                   message.contains("network") ||
                   message.contains("timeout");
        } else if (e instanceof GeneralSecurityException) {
            // Don't retry security errors
            return false;
        }
        
        // Don't retry unknown errors by default
        return false;
    }
    
    /**
     * Get user-friendly error message for display
     */
    public String getUserFriendlyMessage(Exception e, String operation) {
        if (e instanceof GoogleJsonResponseException googleError) {
            int statusCode = googleError.getStatusCode();
            
            return switch (statusCode) {
                case 401 -> "Please check your Google Calendar authentication credentials.";
                case 403 -> "You don't have permission to perform this calendar operation.";
                case 404 -> "The requested calendar event was not found.";
                case 429 -> "Too many requests. Please wait a moment and try again.";
                case 500, 502, 503, 504 -> "Google Calendar service is temporarily unavailable. Please try again later.";
                default -> "Calendar operation failed. Please try again.";
            };
        } else if (e instanceof SocketTimeoutException) {
            return "Calendar operation timed out. Please check your internet connection and try again.";
        } else if (e instanceof UnknownHostException) {
            return "Cannot connect to Google Calendar. Please check your internet connection.";
        } else {
            return "Calendar operation failed. Please try again.";
        }
    }
    
    /**
     * Log error with appropriate level based on severity
     */
    public void logError(Exception e, String operation, int attemptNumber) {
        if (isRetryableError(e)) {
            if (attemptNumber == 1) {
                log.warn("Retryable error during Google Calendar {} (attempt {}): {}", 
                        operation, attemptNumber, e.getMessage());
            } else {
                log.info("Retrying Google Calendar {} (attempt {}): {}", 
                        operation, attemptNumber, e.getMessage());
            }
        } else {
            log.error("Non-retryable error during Google Calendar {}: {}", operation, e.getMessage(), e);
        }
    }
    
    /**
     * Create error metadata for response
     */
    public Map<String, Object> createErrorMetadata(Exception e, String operation, int attemptNumber) {
        return Map.of(
                "operation", operation,
                "attemptNumber", attemptNumber,
                "errorType", e.getClass().getSimpleName(),
                "retryable", isRetryableError(e),
                "timestamp", java.time.LocalDateTime.now().toString()
        );
    }
}