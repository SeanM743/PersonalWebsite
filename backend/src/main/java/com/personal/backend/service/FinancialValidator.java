package com.personal.backend.service;

import com.personal.backend.dto.StockRequest;
import com.personal.backend.util.ValidationResult;
import com.personal.backend.model.StockTicker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class FinancialValidator {
    
    // Stock symbol validation pattern
    private static final Pattern STOCK_SYMBOL_PATTERN = Pattern.compile("^[A-Z]{1,10}$");
    
    // Financial limits
    private static final BigDecimal MIN_PRICE = BigDecimal.valueOf(0.0001);
    private static final BigDecimal MAX_PRICE = BigDecimal.valueOf(1000000);
    private static final BigDecimal MIN_QUANTITY = BigDecimal.valueOf(0.00000001);
    private static final BigDecimal MAX_QUANTITY = BigDecimal.valueOf(1000000000);
    private static final BigDecimal MAX_INVESTMENT = BigDecimal.valueOf(10000000); // $10M per position
    
    // Common invalid symbols
    private static final List<String> INVALID_SYMBOLS = List.of(
            "TEST", "INVALID", "NULL", "UNDEFINED", "ERROR"
    );
    
    /**
     * Validate stock request for creation
     */
    public ValidationResult validateStockRequest(StockRequest request) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (request == null) {
            errors.add("Stock request cannot be null");
            return ValidationResult.invalid(errors);
        }
        
        // Validate symbol
        validateSymbol(request.getSymbol(), errors, warnings);
        
        // Validate purchase price
        validatePrice(request.getPurchasePrice(), "Purchase price", errors, warnings);
        
        // Validate quantity
        validateQuantity(request.getQuantity(), errors, warnings);
        
        // Validate total investment
        if (request.getPurchasePrice() != null && request.getQuantity() != null) {
            validateTotalInvestment(request.getTotalInvestment(), errors, warnings);
        }
        
        // Validate notes
        validateNotes(request.getNotes(), errors, warnings);
        
        return errors.isEmpty() ? 
                ValidationResult.valid(warnings) : 
                ValidationResult.invalid(errors, warnings);
    }
    
    /**
     * Validate stock ticker entity
     */
    public ValidationResult validateStockTicker(StockTicker ticker) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (ticker == null) {
            errors.add("Stock ticker cannot be null");
            return ValidationResult.invalid(errors);
        }
        
        // Validate symbol
        validateSymbol(ticker.getSymbol(), errors, warnings);
        
        // Validate purchase price
        validatePrice(ticker.getPurchasePrice(), "Purchase price", errors, warnings);
        
        // Validate quantity
        validateQuantity(ticker.getQuantity(), errors, warnings);
        
        // Validate user ID
        if (ticker.getUserId() == null || ticker.getUserId() <= 0) {
            errors.add("Valid user ID is required");
        }
        
        // Validate total investment
        if (ticker.hasValidFinancialData()) {
            validateTotalInvestment(ticker.getTotalInvestment(), errors, warnings);
        }
        
        // Validate notes
        validateNotes(ticker.getNotes(), errors, warnings);
        
        return errors.isEmpty() ? 
                ValidationResult.valid(warnings) : 
                ValidationResult.invalid(errors, warnings);
    }
    
    /**
     * Validate stock symbol
     */
    private void validateSymbol(String symbol, List<String> errors, List<String> warnings) {
        if (symbol == null || symbol.trim().isEmpty()) {
            errors.add("Stock symbol is required");
            return;
        }
        
        String trimmedSymbol = symbol.trim().toUpperCase();
        
        if (!STOCK_SYMBOL_PATTERN.matcher(trimmedSymbol).matches()) {
            errors.add("Stock symbol must be 1-10 uppercase letters");
            return;
        }
        
        if (INVALID_SYMBOLS.contains(trimmedSymbol)) {
            errors.add("Invalid stock symbol: " + trimmedSymbol);
            return;
        }
        
        // Warnings for potentially problematic symbols
        if (trimmedSymbol.length() > 5) {
            warnings.add("Symbol is longer than typical (5+ characters): " + trimmedSymbol);
        }
        
        if (trimmedSymbol.endsWith("X") || trimmedSymbol.endsWith("Y")) {
            warnings.add("Symbol may be a preferred stock or special class: " + trimmedSymbol);
        }
    }
    
    /**
     * Validate price value
     */
    private void validatePrice(BigDecimal price, String fieldName, List<String> errors, List<String> warnings) {
        if (price == null) {
            errors.add(fieldName + " is required");
            return;
        }
        
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(fieldName + " must be greater than zero");
            return;
        }
        
        if (price.compareTo(MIN_PRICE) < 0) {
            errors.add(fieldName + " must be at least $" + MIN_PRICE);
            return;
        }
        
        if (price.compareTo(MAX_PRICE) > 0) {
            errors.add(fieldName + " cannot exceed $" + MAX_PRICE);
            return;
        }
        
        // Check decimal places
        if (price.scale() > 4) {
            errors.add(fieldName + " cannot have more than 4 decimal places");
            return;
        }
        
        // Warnings for unusual prices
        if (price.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            warnings.add(fieldName + " is very low (penny stock): $" + price);
        }
        
        if (price.compareTo(BigDecimal.valueOf(1000)) > 0) {
            warnings.add(fieldName + " is very high: $" + price);
        }
    }
    
    /**
     * Validate quantity value
     */
    private void validateQuantity(BigDecimal quantity, List<String> errors, List<String> warnings) {
        if (quantity == null) {
            errors.add("Quantity is required");
            return;
        }
        
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Quantity must be greater than zero");
            return;
        }
        
        if (quantity.compareTo(MIN_QUANTITY) < 0) {
            errors.add("Quantity must be at least " + MIN_QUANTITY);
            return;
        }
        
        if (quantity.compareTo(MAX_QUANTITY) > 0) {
            errors.add("Quantity cannot exceed " + MAX_QUANTITY);
            return;
        }
        
        // Check decimal places
        if (quantity.scale() > 8) {
            errors.add("Quantity cannot have more than 8 decimal places");
            return;
        }
        
        // Warnings for unusual quantities
        if (quantity.compareTo(BigDecimal.valueOf(0.001)) < 0) {
            warnings.add("Quantity is very small: " + quantity);
        }
        
        if (quantity.compareTo(BigDecimal.valueOf(1000000)) > 0) {
            warnings.add("Quantity is very large: " + quantity);
        }
    }
    
    /**
     * Validate total investment amount
     */
    private void validateTotalInvestment(BigDecimal totalInvestment, List<String> errors, List<String> warnings) {
        if (totalInvestment == null) {
            return; // This is calculated, so null is acceptable
        }
        
        if (totalInvestment.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Total investment must be greater than zero");
            return;
        }
        
        if (totalInvestment.compareTo(MAX_INVESTMENT) > 0) {
            errors.add("Total investment cannot exceed $" + MAX_INVESTMENT);
            return;
        }
        
        // Warnings for investment amounts
        if (totalInvestment.compareTo(BigDecimal.valueOf(1)) < 0) {
            warnings.add("Total investment is very small: $" + totalInvestment);
        }
        
        if (totalInvestment.compareTo(BigDecimal.valueOf(100000)) > 0) {
            warnings.add("Total investment is very large: $" + totalInvestment);
        }
    }
    
    /**
     * Validate notes field
     */
    private void validateNotes(String notes, List<String> errors, List<String> warnings) {
        if (notes == null) {
            return; // Notes are optional
        }
        
        if (notes.length() > 500) {
            errors.add("Notes cannot exceed 500 characters");
            return;
        }
        
        // Check for potentially problematic content
        String lowerNotes = notes.toLowerCase();
        if (lowerNotes.contains("insider") || lowerNotes.contains("tip")) {
            warnings.add("Notes contain potentially sensitive information");
        }
    }
    
    /**
     * Validate market data values
     */
    public ValidationResult validateMarketData(String symbol, BigDecimal currentPrice, BigDecimal previousClose) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Validate symbol
        validateSymbol(symbol, errors, warnings);
        
        // Validate current price
        if (currentPrice != null) {
            validatePrice(currentPrice, "Current price", errors, warnings);
        }
        
        // Validate previous close
        if (previousClose != null) {
            validatePrice(previousClose, "Previous close", errors, warnings);
        }
        
        // Validate price relationship
        if (currentPrice != null && previousClose != null) {
            BigDecimal change = currentPrice.subtract(previousClose);
            BigDecimal changePercent = change.divide(previousClose, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            if (changePercent.abs().compareTo(BigDecimal.valueOf(50)) > 0) {
                warnings.add("Large price change detected: " + changePercent + "%");
            }
        }
        
        return errors.isEmpty() ? 
                ValidationResult.valid(warnings) : 
                ValidationResult.invalid(errors, warnings);
    }
    
    /**
     * Validate portfolio limits for user
     */
    public ValidationResult validatePortfolioLimits(Long userId, int currentPositions, int maxPositions, 
                                                   BigDecimal totalInvestment, BigDecimal maxTotalInvestment) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (userId == null || userId <= 0) {
            errors.add("Valid user ID is required");
        }
        
        if (currentPositions >= maxPositions) {
            errors.add("Maximum number of positions reached: " + maxPositions);
        }
        
        if (totalInvestment != null && maxTotalInvestment != null && 
            totalInvestment.compareTo(maxTotalInvestment) > 0) {
            errors.add("Total portfolio investment exceeds maximum: $" + maxTotalInvestment);
        }
        
        // Warnings for approaching limits
        if (currentPositions >= maxPositions * 0.9) {
            warnings.add("Approaching maximum positions limit: " + currentPositions + "/" + maxPositions);
        }
        
        if (totalInvestment != null && maxTotalInvestment != null && 
            totalInvestment.compareTo(maxTotalInvestment.multiply(BigDecimal.valueOf(0.9))) > 0) {
            warnings.add("Approaching maximum investment limit");
        }
        
        return errors.isEmpty() ? 
                ValidationResult.valid(warnings) : 
                ValidationResult.invalid(errors, warnings);
    }
    
    /**
     * Sanitize and normalize stock symbol
     */
    public String sanitizeSymbol(String symbol) {
        if (symbol == null) {
            return null;
        }
        
        return symbol.trim().toUpperCase().replaceAll("[^A-Z]", "");
    }
    
    /**
     * Sanitize notes field
     */
    public String sanitizeNotes(String notes) {
        if (notes == null) {
            return null;
        }
        
        String sanitized = notes.trim();
        if (sanitized.isEmpty()) {
            return null;
        }
        
        // Remove potentially harmful characters
        sanitized = sanitized.replaceAll("[<>\"'&]", "");
        
        // Limit length
        if (sanitized.length() > 500) {
            sanitized = sanitized.substring(0, 500);
        }
        
        return sanitized;
    }
}