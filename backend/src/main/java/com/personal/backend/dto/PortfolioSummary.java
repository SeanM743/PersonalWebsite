package com.personal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PortfolioSummary {
    
    private Long userId;
    private BigDecimal totalValue;
    private BigDecimal totalInvestment;
    private BigDecimal totalGainLoss;
    private BigDecimal totalGainLossPercentage;
    private BigDecimal dailyChange;
    private BigDecimal dailyChangePercentage;
    private Integer totalPositions;
    private Integer positionsUp;
    private Integer positionsDown;
    private Integer positionsFlat;
    private LocalDateTime lastUpdated;
    private Boolean isMarketOpen;
    private List<StockPerformance> topPerformers;
    private List<StockPerformance> worstPerformers;
    private String currency;
    private List<StockPerformance> stockPerformances;
    private Map<String, Object> additionalMetrics;
    private Integer positionsWithCurrentData;
    private BigDecimal dataFreshness;
    
    // Alias methods for backward compatibility
    public BigDecimal getCurrentValue() {
        return totalValue;
    }
    
    public void setCurrentValue(BigDecimal currentValue) {
        this.totalValue = currentValue;
    }
    
    public BigDecimal getDailyChange() {
        return dailyChange;
    }
    
    public BigDecimal getDailyChangePercentage() {
        return dailyChangePercentage;
    }
    
    public List<StockPerformance> getStockPerformances() {
        return stockPerformances;
    }
    
    /**
     * Calculate total gain/loss percentage
     */
    public BigDecimal calculateTotalGainLossPercentage() {
        if (totalGainLoss == null || totalInvestment == null || totalInvestment.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return totalGainLoss.divide(totalInvestment, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Calculate daily change percentage
     */
    public BigDecimal calculateDailyChangePercentage() {
        if (dailyChange == null || totalValue == null || totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal previousValue = totalValue.subtract(dailyChange);
        if (previousValue.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return dailyChange.divide(previousValue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Check if portfolio is up overall
     */
    public Boolean isPortfolioUp() {
        if (totalGainLoss == null) {
            return null;
        }
        return totalGainLoss.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if portfolio is up for the day
     */
    public Boolean isDailyUp() {
        if (dailyChange == null) {
            return null;
        }
        return dailyChange.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get formatted total value
     */
    public String getFormattedTotalValue() {
        if (totalValue == null) {
            return "N/A";
        }
        return String.format("$%,.2f", totalValue);
    }
    
    /**
     * Get formatted total gain/loss with sign and percentage
     */
    public String getFormattedTotalGainLoss() {
        if (totalGainLoss == null) {
            return "N/A";
        }
        
        BigDecimal percentage = getTotalGainLossPercentage() != null ? 
                getTotalGainLossPercentage() : calculateTotalGainLossPercentage();
        
        String sign = totalGainLoss.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        String percentageStr = percentage != null ? String.format(" (%s%.2f%%)", sign, percentage.abs()) : "";
        
        return String.format("%s$%,.2f%s", sign, totalGainLoss.abs(), percentageStr);
    }
    
    /**
     * Get formatted daily change with sign and percentage
     */
    public String getFormattedDailyChange() {
        if (dailyChange == null) {
            return "N/A";
        }
        
        BigDecimal percentage = getDailyChangePercentage() != null ? 
                getDailyChangePercentage() : calculateDailyChangePercentage();
        
        String sign = dailyChange.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        String percentageStr = percentage != null ? String.format(" (%s%.2f%%)", sign, percentage.abs()) : "";
        
        return String.format("%s$%,.2f%s", sign, dailyChange.abs(), percentageStr);
    }
    
    /**
     * Get portfolio diversity score (0-100)
     */
    public Integer getDiversityScore() {
        if (totalPositions == null || totalPositions == 0) {
            return 0;
        }
        
        // Simple diversity score based on number of positions
        // More sophisticated calculation could consider sector distribution
        if (totalPositions >= 20) return 100;
        if (totalPositions >= 15) return 85;
        if (totalPositions >= 10) return 70;
        if (totalPositions >= 5) return 50;
        if (totalPositions >= 3) return 30;
        return 15;
    }
    
    /**
     * Check if portfolio data is fresh
     */
    public boolean isFresh() {
        if (lastUpdated == null) {
            return false;
        }
        return lastUpdated.isAfter(LocalDateTime.now().minusMinutes(15));
    }
    
    /**
     * Get market status description
     */
    public String getMarketStatusDescription() {
        if (isMarketOpen == null) {
            return "Market status unknown";
        }
        return isMarketOpen ? "Market is open" : "Market is closed";
    }
    
    @Override
    public String toString() {
        return String.format("PortfolioSummary{totalValue=%s, totalGainLoss=%s, positions=%d, lastUpdated=%s}", 
                getFormattedTotalValue(), getFormattedTotalGainLoss(), totalPositions, lastUpdated);
    }
}