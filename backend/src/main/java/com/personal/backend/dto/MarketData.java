package com.personal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MarketData {
    
    private String symbol;
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal dailyChange;
    private BigDecimal dailyChangePercentage;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal open;
    private Long volume;
    private LocalDateTime timestamp;
    private Boolean isMarketOpen;
    private String currency;
    private String exchange;
    
    // Additional fields for Finnhub integration
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private LocalDateTime lastUpdated;
    private String dataSource;
    private Boolean hasError;
    private String errorMessage;
    
    // Cache-related fields
    private LocalDateTime cacheTimestamp;
    private Integer cacheTtlMinutes;
    
    /**
     * Check if market data has errors
     */
    public boolean hasError() {
        return hasError != null && hasError;
    }
    
    /**
     * Calculate daily change from current price and previous close
     */
    public BigDecimal calculateDailyChange() {
        if (currentPrice == null || previousClose == null) {
            return null;
        }
        return currentPrice.subtract(previousClose);
    }
    
    /**
     * Calculate daily change percentage
     */
    public BigDecimal calculateDailyChangePercentage() {
        BigDecimal change = calculateDailyChange();
        if (change == null || previousClose == null || previousClose.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return change.divide(previousClose, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Check if market data is valid
     */
    public boolean isValid() {
        return symbol != null && 
               !symbol.trim().isEmpty() && 
               currentPrice != null && 
               currentPrice.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if market data is fresh (within last hour)
     */
    public boolean isFresh() {
        if (timestamp == null) {
            return false;
        }
        return timestamp.isAfter(LocalDateTime.now().minusHours(1));
    }
    
    /**
     * Get formatted price string
     */
    public String getFormattedPrice() {
        if (currentPrice == null) {
            return "N/A";
        }
        return String.format("$%.2f", currentPrice);
    }
    
    /**
     * Get formatted daily change string with sign
     */
    public String getFormattedDailyChange() {
        BigDecimal change = getDailyChange() != null ? getDailyChange() : calculateDailyChange();
        BigDecimal changePercent = getDailyChangePercentage() != null ? getDailyChangePercentage() : calculateDailyChangePercentage();
        
        if (change == null || changePercent == null) {
            return "N/A";
        }
        
        String sign = change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return String.format("%s$%.2f (%s%.2f%%)", sign, change.abs(), sign, changePercent.abs());
    }
    
    /**
     * Check if stock is up for the day
     */
    public Boolean isUp() {
        BigDecimal change = getDailyChange() != null ? getDailyChange() : calculateDailyChange();
        if (change == null) {
            return null;
        }
        return change.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if stock is down for the day
     */
    public Boolean isDown() {
        BigDecimal change = getDailyChange() != null ? getDailyChange() : calculateDailyChange();
        if (change == null) {
            return null;
        }
        return change.compareTo(BigDecimal.ZERO) < 0;
    }
    
    @Override
    public String toString() {
        return String.format("MarketData{symbol='%s', price=%s, change=%s, timestamp=%s}", 
                symbol, getFormattedPrice(), getFormattedDailyChange(), timestamp);
    }
}