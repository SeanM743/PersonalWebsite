package com.personal.backend.exception;

import com.personal.backend.dto.ContentResponse;
import com.personal.backend.service.ErrorHandlingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    
    private final ErrorHandlingService errorHandlingService;
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ContentResponse<Void>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ContentResponse<Void> response = errorHandlingService.handleValidationErrors(errors);
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ContentResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        log.warn("Invalid argument: {}", ex.getMessage());
        ContentResponse<Void> response = ContentResponse.error("Invalid input: " + ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ContentResponse<Void>> handleDataAccessException(
            DataAccessException ex, WebRequest request) {
        
        log.error("Database error", ex);
        ContentResponse<Void> response = ContentResponse.error(
                "Database error occurred. Please try again later.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ContentResponse<Void>> handleRestClientException(
            RestClientException ex, WebRequest request) {
        
        log.warn("External API error: {}", ex.getMessage());
        ContentResponse<Void> response = errorHandlingService.handleExternalApiError(ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
    
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ContentResponse<Void>> handleTimeoutException(
            TimeoutException ex, WebRequest request) {
        
        log.warn("Operation timeout: {}", ex.getMessage());
        ContentResponse<Void> response = ContentResponse.error(
                "Operation timed out. Please try again.");
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(response);
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ContentResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, WebRequest request) {
        
        log.warn("File upload size exceeded: {}", ex.getMessage());
        ContentResponse<Void> response = ContentResponse.error(
                "File size exceeds maximum allowed limit.");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }
    
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ContentResponse<Void>> handleSecurityException(
            SecurityException ex, WebRequest request) {
        
        log.error("Security violation: {}", ex.getMessage());
        ContentResponse<Void> response = ContentResponse.error(
                "Access denied. Insufficient permissions.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ContentResponse<Void>> handleGenericException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected error", ex);
        ContentResponse<Void> response = ContentResponse.error(
                "An unexpected error occurred. Please try again later.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}