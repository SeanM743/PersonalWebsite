package com.personal.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_tickers", indexes = {
        @Index(name = "idx_stock_symbol", columnList = "symbol"),
        @Index(name = "idx_stock_user_id", columnList = "userId"),
        @Index(name = "idx_stock_created_at", columnList = "createdAt"),
        @Index(name = "idx_stock_user_symbol", columnList = "userId, symbol")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTicker {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 10)
    @NotBlank(message = "Stock symbol is required")
    @Pattern(regexp = "^[A-Z]{1,10}$", message = "Stock symbol must be 1-10 uppercase letters")
    private String symbol;
    
    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Purchase price is required")
    @DecimalMin(value = "0.0001", message = "Purchase price must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Purchase price must have at most 4 decimal places")
    private BigDecimal purchasePrice;
    
    @Column(nullable = false, precision = 19, scale = 8)
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.00000001", message = "Quantity must be greater than 0")
    @Digits(integer = 11, fraction = 8, message = "Quantity must have at most 8 decimal places")
    private BigDecimal quantity;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(length = 500)
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Market Data Fields (Persisted)
    @Column(precision = 19, scale = 4)
    private BigDecimal currentPrice;
    
    @Transient // Calculated from currentPrice * quantity
    private BigDecimal currentValue;
    
    @Transient // Calculated
    private BigDecimal totalGainLoss;
    
    @Transient // Calculated
    private BigDecimal totalGainLossPercentage;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal dailyChange;
    
    @Column(precision = 10, scale = 4)
    private BigDecimal dailyChangePercentage;
    
    @Column
    private LocalDateTime lastPriceUpdate;
    
    @Column
    private Boolean isMarketOpen;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
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
     * Calculate current value if current price is available
     */
    public BigDecimal getCurrentValue() {
        if (currentPrice == null || quantity == null) {
            return null;
        }
        return currentPrice.multiply(quantity);
    }
    
    /**
     * Calculate total gain/loss if current price is available
     */
    public BigDecimal getTotalGainLoss() {
        BigDecimal currentVal = getCurrentValue();
        if (currentVal == null) {
            return null;
        }
        return currentVal.subtract(getTotalInvestment());
    }
    
    /**
     * Calculate total gain/loss percentage if current price is available
     */
    public BigDecimal getTotalGainLossPercentage() {
        BigDecimal gainLoss = getTotalGainLoss();
        BigDecimal investment = getTotalInvestment();
        
        if (gainLoss == null || investment.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        
        return gainLoss.divide(investment, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Check if this stock has valid financial data
     */
    public boolean hasValidFinancialData() {
        return purchasePrice != null && 
               quantity != null && 
               purchasePrice.compareTo(BigDecimal.ZERO) > 0 && 
               quantity.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if current market data is available
     */
    public boolean hasCurrentMarketData() {
        return currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if market data is fresh (updated within last hour)
     */
    public boolean isMarketDataFresh() {
        if (lastPriceUpdate == null) {
            return false;
        }
        return lastPriceUpdate.isAfter(LocalDateTime.now().minusHours(1));
    }
    
    @Override
    public String toString() {
        return String.format("StockTicker{symbol='%s', quantity=%s, purchasePrice=%s, userId=%d}", 
                symbol, quantity, purchasePrice, userId);
    }
}