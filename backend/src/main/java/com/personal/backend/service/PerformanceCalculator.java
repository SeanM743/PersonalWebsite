package com.personal.backend.service;

import com.personal.backend.dto.MarketData;
import com.personal.backend.dto.StockPerformance;
import com.personal.backend.model.StockTicker;
import com.personal.backend.util.FinancialCalculationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Financial performance calculations with mathematical precision
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceCalculator {
    
    private final FinancialValidator financialValidator;
    
    /**
     * Calculate portfolio value based on current market prices
     */
    public BigDecimal calculatePortfolioValue(List<StockTicker> holdings, Map<String, MarketData> marketDataMap) {
        if (holdings == null || holdings.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalValue = BigDecimal.ZERO;
        
        for (StockTicker holding : holdings) {
            if (!holding.hasValidFinancialData()) {
                log.warn("Skipping invalid holding data for symbol: {}", holding.getSymbol());
                continue;
            }
            
            MarketData marketData = marketDataMap.get(holding.getSymbol());
            if (marketData != null && marketData.getCurrentPrice() != null && !marketData.hasError()) {
                BigDecimal currentValue = FinancialCalculationUtil.multiply(
                        holding.getQuantity(), 
                        marketData.getCurrentPrice()
                );
                totalValue = FinancialCalculationUtil.add(totalValue, currentValue);
            } else {
                // Use purchase price as fallback if no current market data
                BigDecimal fallbackValue = holding.getTotalInvestment();
                totalValue = FinancialCalculationUtil.add(totalValue, fallbackValue);
                log.debug("Using purchase price fallback for symbol: {}", holding.getSymbol());
            }
        }
        
        return totalValue;
    }
    
    /**
     * Calculate individual stock gain/loss with precision
     */
    public StockPerformance calculateStockPerformance(StockTicker holding, MarketData marketData) {
        if (holding == null || !holding.hasValidFinancialData()) {
            return createErrorPerformance(holding != null ? holding.getSymbol() : "UNKNOWN", 
                    "Invalid holding data");
        }
        
        String symbol = holding.getSymbol();
        BigDecimal quantity = holding.getQuantity();
        BigDecimal purchasePrice = holding.getPurchasePrice();
        BigDecimal totalInvestment = holding.getTotalInvestment();
        
        StockPerformance.StockPerformanceBuilder builder = StockPerformance.builder()
                .symbol(symbol)
                .quantity(quantity)
                .purchasePrice(purchasePrice)
                .totalInvestment(totalInvestment)
                .lastUpdated(LocalDateTime.now());
        
        if (marketData == null || marketData.hasError() || marketData.getCurrentPrice() == null) {
            // No current market data available
            return builder
                    .currentPrice(BigDecimal.ZERO)
                    .currentValue(BigDecimal.ZERO)
                    .totalGainLoss(BigDecimal.ZERO)
                    .totalGainLossPercentage(BigDecimal.ZERO)
                    .dailyChange(BigDecimal.ZERO)
                    .dailyChangePercentage(BigDecimal.ZERO)
                    .hasCurrentData(false)
                    .errorMessage(marketData != null ? marketData.getErrorMessage() : "No market data available")
                    .build();
        }
        
        BigDecimal currentPrice = marketData.getCurrentPrice();
        
        // Validate current price
        if (currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return createErrorPerformance(symbol, "Invalid current price: " + currentPrice);
        }
        
        // Calculate current value
        BigDecimal currentValue = FinancialCalculationUtil.multiply(quantity, currentPrice);
        
        // Calculate total gain/loss
        BigDecimal totalGainLoss = FinancialCalculationUtil.subtract(currentValue, totalInvestment);
        
        // Calculate total gain/loss percentage
        BigDecimal totalGainLossPercentage = BigDecimal.ZERO;
        if (totalInvestment.compareTo(BigDecimal.ZERO) > 0) {
            totalGainLossPercentage = FinancialCalculationUtil.divide(totalGainLoss, totalInvestment, FinancialCalculationUtil.PERCENTAGE_SCALE)
                    .multiply(FinancialCalculationUtil.HUNDRED);
        }
        
        // Calculate daily change
        BigDecimal dailyChange = BigDecimal.ZERO;
        BigDecimal dailyChangePercentage = BigDecimal.ZERO;
        
        if (marketData.getDailyChange() != null) {
            // Daily change per share
            BigDecimal dailyChangePerShare = marketData.getDailyChange();
            dailyChange = FinancialCalculationUtil.multiply(quantity, dailyChangePerShare);
            
            if (marketData.getDailyChangePercentage() != null) {
                dailyChangePercentage = marketData.getDailyChangePercentage();
            } else if (marketData.getPreviousClose() != null && 
                      marketData.getPreviousClose().compareTo(BigDecimal.ZERO) > 0) {
                dailyChangePercentage = FinancialCalculationUtil.divide(dailyChangePerShare, marketData.getPreviousClose(), FinancialCalculationUtil.PERCENTAGE_SCALE)
                        .multiply(FinancialCalculationUtil.HUNDRED);
            }
        }
        
        return builder
                .currentPrice(currentPrice)
                .currentValue(currentValue)
                .totalGainLoss(totalGainLoss)
                .totalGainLossPercentage(totalGainLossPercentage)
                .dailyChange(dailyChange)
                .dailyChangePercentage(dailyChangePercentage)
                .hasCurrentData(true)
                .isMarketOpen(marketData.getIsMarketOpen())
                .lastPriceUpdate(marketData.getLastUpdated())
                .build();
    }
    
    /**
     * Calculate daily change for portfolio
     */
    public BigDecimal calculatePortfolioDailyChange(List<StockTicker> holdings, Map<String, MarketData> marketDataMap) {
        if (holdings == null || holdings.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalDailyChange = BigDecimal.ZERO;
        
        for (StockTicker holding : holdings) {
            if (!holding.hasValidFinancialData()) {
                continue;
            }
            
            MarketData marketData = marketDataMap.get(holding.getSymbol());
            if (marketData != null && marketData.getDailyChange() != null && !marketData.hasError()) {
                BigDecimal stockDailyChange = FinancialCalculationUtil.multiply(
                        holding.getQuantity(), 
                        marketData.getDailyChange()
                );
                totalDailyChange = FinancialCalculationUtil.add(totalDailyChange, stockDailyChange);
            }
        }
        
        return totalDailyChange;
    }
    
    /**
     * Calculate portfolio daily change percentage
     */
    public BigDecimal calculatePortfolioDailyChangePercentage(List<StockTicker> holdings, Map<String, MarketData> marketDataMap) {
        BigDecimal dailyChange = calculatePortfolioDailyChange(holdings, marketDataMap);
        BigDecimal previousValue = calculatePreviousPortfolioValue(holdings, marketDataMap);
        
        if (previousValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        return FinancialCalculationUtil.divide(dailyChange, previousValue, FinancialCalculationUtil.PERCENTAGE_SCALE)
                .multiply(FinancialCalculationUtil.HUNDRED);
    }
    
    /**
     * Calculate total portfolio gain/loss
     */
    public BigDecimal calculatePortfolioGainLoss(List<StockTicker> holdings, Map<String, MarketData> marketDataMap) {
        BigDecimal currentValue = calculatePortfolioValue(holdings, marketDataMap);
        BigDecimal totalInvestment = calculateTotalInvestment(holdings);
        
        return FinancialCalculationUtil.subtract(currentValue, totalInvestment);
    }
    
    /**
     * Calculate total portfolio gain/loss percentage
     */
    public BigDecimal calculatePortfolioGainLossPercentage(List<StockTicker> holdings, Map<String, MarketData> marketDataMap) {
        BigDecimal gainLoss = calculatePortfolioGainLoss(holdings, marketDataMap);
        BigDecimal totalInvestment = calculateTotalInvestment(holdings);
        
        if (totalInvestment.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        return FinancialCalculationUtil.divide(gainLoss, totalInvestment, FinancialCalculationUtil.PERCENTAGE_SCALE)
                .multiply(FinancialCalculationUtil.HUNDRED);
    }
    
    /**
     * Calculate total investment amount
     */
    public BigDecimal calculateTotalInvestment(List<StockTicker> holdings) {
        if (holdings == null || holdings.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return holdings.stream()
                .filter(holding -> holding.hasValidFinancialData())
                .map(StockTicker::getTotalInvestment)
                .reduce(BigDecimal.ZERO, FinancialCalculationUtil::add);
    }
    
    /**
     * Calculate previous portfolio value (for daily change calculation)
     */
    private BigDecimal calculatePreviousPortfolioValue(List<StockTicker> holdings, Map<String, MarketData> marketDataMap) {
        if (holdings == null || holdings.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal previousValue = BigDecimal.ZERO;
        
        for (StockTicker holding : holdings) {
            if (!holding.hasValidFinancialData()) {
                continue;
            }
            
            MarketData marketData = marketDataMap.get(holding.getSymbol());
            if (marketData != null && marketData.getPreviousClose() != null && !marketData.hasError()) {
                BigDecimal previousStockValue = FinancialCalculationUtil.multiply(
                        holding.getQuantity(), 
                        marketData.getPreviousClose()
                );
                previousValue = FinancialCalculationUtil.add(previousValue, previousStockValue);
            } else {
                // Use current price minus daily change as fallback
                if (marketData != null && marketData.getCurrentPrice() != null && marketData.getDailyChange() != null) {
                    BigDecimal previousPrice = FinancialCalculationUtil.subtract(
                            marketData.getCurrentPrice(), 
                            marketData.getDailyChange()
                    );
                    BigDecimal previousStockValue = FinancialCalculationUtil.multiply(
                            holding.getQuantity(), 
                            previousPrice
                    );
                    previousValue = FinancialCalculationUtil.add(previousValue, previousStockValue);
                }
            }
        }
        
        return previousValue;
    }
    
    /**
     * Batch calculate performance for multiple holdings
     */
    public List<StockPerformance> calculateBatchPerformance(List<StockTicker> holdings, Map<String, MarketData> marketDataMap) {
        if (holdings == null || holdings.isEmpty()) {
            return List.of();
        }
        
        return holdings.stream()
                .map(holding -> {
                    MarketData marketData = marketDataMap.get(holding.getSymbol());
                    return calculateStockPerformance(holding, marketData);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Calculate performance metrics with error handling
     */
    public PerformanceMetrics calculatePerformanceMetrics(List<StockTicker> holdings, Map<String, MarketData> marketDataMap) {
        try {
            BigDecimal totalInvestment = calculateTotalInvestment(holdings);
            BigDecimal currentValue = calculatePortfolioValue(holdings, marketDataMap);
            BigDecimal totalGainLoss = calculatePortfolioGainLoss(holdings, marketDataMap);
            BigDecimal totalGainLossPercentage = calculatePortfolioGainLossPercentage(holdings, marketDataMap);
            BigDecimal dailyChange = calculatePortfolioDailyChange(holdings, marketDataMap);
            BigDecimal dailyChangePercentage = calculatePortfolioDailyChangePercentage(holdings, marketDataMap);
            
            int totalPositions = holdings.size();
            int positionsWithData = (int) holdings.stream()
                    .filter(holding -> marketDataMap.containsKey(holding.getSymbol()) && 
                            !marketDataMap.get(holding.getSymbol()).hasError())
                    .count();
            
            return new PerformanceMetrics(
                    totalInvestment,
                    currentValue,
                    totalGainLoss,
                    totalGainLossPercentage,
                    dailyChange,
                    dailyChangePercentage,
                    totalPositions,
                    positionsWithData,
                    LocalDateTime.now()
            );
            
        } catch (Exception e) {
            log.error("Error calculating performance metrics: {}", e.getMessage(), e);
            return new PerformanceMetrics(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, LocalDateTime.now()
            );
        }
    }
    
    /**
     * Create error performance object
     */
    private StockPerformance createErrorPerformance(String symbol, String errorMessage) {
        return StockPerformance.builder()
                .symbol(symbol)
                .currentPrice(BigDecimal.ZERO)
                .currentValue(BigDecimal.ZERO)
                .totalGainLoss(BigDecimal.ZERO)
                .totalGainLossPercentage(BigDecimal.ZERO)
                .dailyChange(BigDecimal.ZERO)
                .dailyChangePercentage(BigDecimal.ZERO)
                .hasCurrentData(false)
                .errorMessage(errorMessage)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
    
    /**
     * Performance metrics record
     */
    public record PerformanceMetrics(
            BigDecimal totalInvestment,
            BigDecimal currentValue,
            BigDecimal totalGainLoss,
            BigDecimal totalGainLossPercentage,
            BigDecimal dailyChange,
            BigDecimal dailyChangePercentage,
            int totalPositions,
            int positionsWithData,
            LocalDateTime calculatedAt
    ) {}
}