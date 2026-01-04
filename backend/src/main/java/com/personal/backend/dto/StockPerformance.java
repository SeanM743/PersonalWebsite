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
@Builder
public class StockPerformance {
    
    private String symbol;
    private BigDecimal currentPrice;
    private BigDecimal purchasePrice;
    private BigDecimal quantity;
    private BigDecimal currentValue;
    private BigDecimal totalInvestment;
    private BigDecimal totalGainLoss;
    private BigDecimal totalGainLossPercentage;
    private BigDecimal dailyChange;
    private BigDecimal dailyChangePercentage;
    private LocalDateTime lastUpdated;
    private LocalDateTime lastPriceUpdate;
    private Boolean isMarketOpen;
    private Boolean hasCurrentData;
    private String errorMessage;
    private String notes;
    
    /**
     * Calculate current value
     */
    public BigDecimal calculateCurrentValue() {
        if (currentPrice == null || quantity == null) {
            return null;
        }
        return currentPrice.multiply(quantity);
    }
    
    /**
     * Calculate total investment
     */
    public BigDecimal calculateTotalInvestment() {
        if (purchasePrice == null || quantity == null) {
            return null;
        }
        return purchasePrice.multiply(quantity);
    }
    
    /**
     * Calculate total gain/loss
     */
    public BigDecimal calculateTotalGainLoss() {
        BigDecimal current = getCurrentValue() != null ? getCurrentValue() : calculateCurrentValue();
        BigDecimal investment = getTotalInvestment() != null ? getTotalInvestment() : calculateTotalInvestment();
        
        if (current == null || investment == null) {
            return null;
        }
        return current.subtract(investment);
    }
    
    /**
     * Calculate total gain/loss percentage
     */
    public BigDecimal calculateTotalGainLossPercentage() {
        BigDecimal gainLoss = getTotalGainLoss() != null ? getTotalGainLoss() : calculateTotalGainLoss();
        BigDecimal investment = getTotalInvestment() != null ? getTotalInvestment() : calculateTotalInvestment();
        
        if (gainLoss == null || investment == null || investment.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        
        return gainLoss.divide(investment, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Check if position is profitable
     */
    public Boolean isProfitable() {
        BigDecimal gainLoss = getTotalGainLoss() != null ? getTotalGainLoss() : calculateTotalGainLoss();
        if (gainLoss == null) {
            return null;
        }
        return gainLoss.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if stock is up for the day
     */
    public Boolean isDailyUp() {
        if (dailyChange == null) {
            return null;
        }
        return dailyChange.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get formatted current price
     */
    public String getFormattedCurrentPrice() {
        if (currentPrice == null) {
            return "N/A";
        }
        return String.format("$%.2f", currentPrice);
    }
    
    /**
     * Get formatted current value
     */
    public String getFormattedCurrentValue() {
        BigDecimal value = getCurrentValue() != null ? getCurrentValue() : calculateCurrentValue();
        if (value == null) {
            return "N/A";
        }
        return String.format("$%,.2f", value);
    }
    
    /**
     * Get formatted total gain/loss with sign and percentage
     */
    public String getFormattedTotalGainLoss() {
        BigDecimal gainLoss = getTotalGainLoss() != null ? getTotalGainLoss() : calculateTotalGainLoss();
        BigDecimal percentage = getTotalGainLossPercentage() != null ? 
                getTotalGainLossPercentage() : calculateTotalGainLossPercentage();
        
        if (gainLoss == null) {
            return "N/A";
        }
        
        String sign = gainLoss.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        String percentageStr = percentage != null ? String.format(" (%s%.2f%%)", sign, percentage.abs()) : "";
        
        return String.format("%s$%,.2f%s", sign, gainLoss.abs(), percentageStr);
    }
    
    /**
     * Get formatted daily change with sign and percentage
     */
    public String getFormattedDailyChange() {
        if (dailyChange == null) {
            return "N/A";
        }
        
        String sign = dailyChange.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        String percentageStr = dailyChangePercentage != null ? 
                String.format(" (%s%.2f%%)", sign, dailyChangePercentage.abs()) : "";
        
        return String.format("%s$%.2f%s", sign, dailyChange.abs(), percentageStr);
    }
    
    /**
     * Get performance category
     */
    public String getPerformanceCategory() {
        BigDecimal percentage = getTotalGainLossPercentage() != null ? 
                getTotalGainLossPercentage() : calculateTotalGainLossPercentage();
        
        if (percentage == null) {
            return "Unknown";
        }
        
        if (percentage.compareTo(BigDecimal.valueOf(20)) >= 0) return "Excellent";
        if (percentage.compareTo(BigDecimal.valueOf(10)) >= 0) return "Good";
        if (percentage.compareTo(BigDecimal.valueOf(0)) > 0) return "Positive";
        if (percentage.compareTo(BigDecimal.valueOf(0)) == 0) return "Flat";
        if (percentage.compareTo(BigDecimal.valueOf(-10)) >= 0) return "Negative";
        if (percentage.compareTo(BigDecimal.valueOf(-20)) >= 0) return "Poor";
        return "Very Poor";
    }
    
    /**
     * Check if data is fresh
     */
    public boolean isFresh() {
        if (lastUpdated == null) {
            return false;
        }
        return lastUpdated.isAfter(LocalDateTime.now().minusMinutes(15));
    }
    
    @Override
    public String toString() {
        return String.format("StockPerformance{symbol='%s', currentPrice=%s, gainLoss=%s, category=%s}", 
                symbol, getFormattedCurrentPrice(), getFormattedTotalGainLoss(), getPerformanceCategory());
    }
}