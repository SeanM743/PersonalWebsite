package com.personal.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockRequest {
    
    @NotBlank(message = "Stock symbol is required")
    @Pattern(regexp = "^[A-Z]{1,10}$", message = "Stock symbol must be 1-10 uppercase letters")
    private String symbol;
    
    @NotNull(message = "Purchase price is required")
    @DecimalMin(value = "0.0001", message = "Purchase price must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Purchase price must have at most 4 decimal places")
    private BigDecimal purchasePrice;
    
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.00000001", message = "Quantity must be greater than 0")
    @Digits(integer = 11, fraction = 8, message = "Quantity must have at most 8 decimal places")
    private BigDecimal quantity;
    
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
    
    /**
     * Calculate total investment amount
     */
    public BigDecimal getTotalInvestment() {
        if (purchasePrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return purchasePrice.multiply(quantity);
    }
    
    /**
     * Validate that the request has valid financial data
     */
    public boolean isValid() {
        return symbol != null && 
               !symbol.trim().isEmpty() && 
               purchasePrice != null && 
               quantity != null && 
               purchasePrice.compareTo(BigDecimal.ZERO) > 0 && 
               quantity.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Normalize symbol to uppercase
     */
    public void normalizeSymbol() {
        if (symbol != null) {
            symbol = symbol.trim().toUpperCase();
        }
    }
    
    /**
     * Trim notes
     */
    public void normalizeNotes() {
        if (notes != null) {
            notes = notes.trim();
            if (notes.isEmpty()) {
                notes = null;
            }
        }
    }
    
    /**
     * Normalize all fields
     */
    public void normalize() {
        normalizeSymbol();
        normalizeNotes();
    }
    
    @Override
    public String toString() {
        return String.format("StockRequest{symbol='%s', quantity=%s, purchasePrice=%s, totalInvestment=%s}", 
                symbol, quantity, purchasePrice, getTotalInvestment());
    }
}