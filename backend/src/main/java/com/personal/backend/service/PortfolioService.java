package com.personal.backend.service;

import com.personal.backend.dto.MarketData;
import com.personal.backend.dto.PortfolioResponse;
import com.personal.backend.dto.PortfolioSummary;
import com.personal.backend.dto.StockPerformance;
import com.personal.backend.model.StockTicker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Unified portfolio operations service orchestrating holdings, market data, and performance calculations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PortfolioService {
    
    private final StockHoldingService stockHoldingService;
    private final FinnhubClient finnhubClient;
    private final MarketDataCacheManager cacheManager;
    private final PerformanceCalculator performanceCalculator;
    private final MarketDataScheduler marketDataScheduler;
    private final FinnhubErrorHandler errorHandler;
    
    @Value("${portfolio.performance.cache.ttl.minutes:5}")
    private int performanceCacheTtlMinutes;
    
    @Value("${portfolio.market.data.timeout.seconds:30}")
    private int marketDataTimeoutSeconds;
    
    @Value("${portfolio.enable.real.time.updates:true}")
    private boolean enableRealTimeUpdates;
    
    /**
     * Get complete portfolio summary with real-time market data
     */
    @Transactional(readOnly = true)
    public PortfolioResponse<PortfolioSummary> getPortfolioSummary(Long userId) {
        try {
            log.info("Generating portfolio summary for user {}", userId);
            
            // Get user's stock holdings
            PortfolioResponse<List<StockTicker>> holdingsResponse = stockHoldingService.getUserStockHoldings(userId);
            if (!holdingsResponse.isSuccess()) {
                return PortfolioResponse.error("Failed to retrieve holdings: " + holdingsResponse.getError());
            }
            
            List<StockTicker> holdings = holdingsResponse.getData();
            if (holdings.isEmpty()) {
                return PortfolioResponse.success(createEmptyPortfolioSummary(userId), "Portfolio is empty");
            }
            
            // Get market data for all symbols
            List<String> symbols = holdings.stream()
                    .map(StockTicker::getSymbol)
                    .toList();
            
            Map<String, MarketData> marketDataMap = getMarketDataForSymbols(symbols);
            
            // Calculate performance metrics
            PerformanceCalculator.PerformanceMetrics metrics = performanceCalculator.calculatePerformanceMetrics(holdings, marketDataMap);
            List<StockPerformance> stockPerformances = performanceCalculator.calculateBatchPerformance(holdings, marketDataMap);
            
            // Build portfolio summary
            PortfolioSummary summary = PortfolioSummary.builder()
                    .userId(userId)
                    .totalInvestment(metrics.totalInvestment())
                    .totalValue(metrics.currentValue())
                    .totalGainLoss(metrics.totalGainLoss())
                    .totalGainLossPercentage(metrics.totalGainLossPercentage())
                    .dailyChange(metrics.dailyChange())
                    .dailyChangePercentage(metrics.dailyChangePercentage())
                    .totalPositions(metrics.totalPositions())
                    .positionsWithCurrentData(metrics.positionsWithData())
                    .stockPerformances(stockPerformances)
                    .lastUpdated(LocalDateTime.now())
                    .isMarketOpen(isAnyMarketOpen(marketDataMap))
                    .dataFreshness(calculateDataFreshness(marketDataMap))
                    .build();
            
            log.info("Portfolio summary generated for user {}: {} positions, ${} total value", 
                    userId, summary.getTotalPositions(), summary.getCurrentValue());
            
            return PortfolioResponse.success(summary, "Portfolio summary generated successfully");
            
        } catch (Exception e) {
            log.error("Error generating portfolio summary for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Failed to generate portfolio summary: " + e.getMessage());
        }
    }
    
    /**
     * Get portfolio summary with detailed stock performance
     */
    @Transactional(readOnly = true)
    public PortfolioResponse<PortfolioSummary> getDetailedPortfolioSummary(Long userId) {
        try {
            log.info("Generating detailed portfolio summary for user {}", userId);
            
            // Get basic portfolio summary
            PortfolioResponse<PortfolioSummary> summaryResponse = getPortfolioSummary(userId);
            if (!summaryResponse.isSuccess()) {
                return summaryResponse;
            }
            
            PortfolioSummary summary = summaryResponse.getData();
            
            // Add additional portfolio analytics
            Map<String, Object> additionalMetrics = calculateAdditionalMetrics(userId, summary);
            
            PortfolioSummary detailedSummary = summary.toBuilder()
                    .additionalMetrics(additionalMetrics)
                    .build();
            
            return PortfolioResponse.success(detailedSummary, "Detailed portfolio summary generated successfully");
            
        } catch (Exception e) {
            log.error("Error generating detailed portfolio summary for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Failed to generate detailed portfolio summary: " + e.getMessage());
        }
    }
    
    /**
     * Refresh portfolio data with latest market information
     */
    public PortfolioResponse<PortfolioSummary> refreshPortfolioData(Long userId) {
        try {
            log.info("Refreshing portfolio data for user {}", userId);
            
            // Get user's symbols
            List<String> symbols = stockHoldingService.getUserSymbols(userId);
            if (symbols.isEmpty()) {
                return getPortfolioSummary(userId);
            }
            
            // Force refresh market data
            refreshMarketDataForSymbols(symbols);
            
            // Generate updated portfolio summary
            return getPortfolioSummary(userId);
            
        } catch (Exception e) {
            log.error("Error refreshing portfolio data for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Failed to refresh portfolio data: " + e.getMessage());
        }
    }
    
    /**
     * Get portfolio performance for specific time period
     */
    @Transactional(readOnly = true)
    public PortfolioResponse<Map<String, Object>> getPortfolioPerformance(Long userId, String period) {
        try {
            log.info("Calculating portfolio performance for user {} over period {}", userId, period);
            
            // Get current portfolio summary
            PortfolioResponse<PortfolioSummary> summaryResponse = getPortfolioSummary(userId);
            if (!summaryResponse.isSuccess()) {
                return PortfolioResponse.error("Failed to get portfolio data: " + summaryResponse.getError());
            }
            
            PortfolioSummary summary = summaryResponse.getData();
            
            // Calculate performance metrics for the specified period
            Map<String, Object> performanceData = Map.of(
                    "period", period,
                    "currentValue", summary.getCurrentValue(),
                    "totalInvestment", summary.getTotalInvestment(),
                    "totalGainLoss", summary.getTotalGainLoss(),
                    "totalGainLossPercentage", summary.getTotalGainLossPercentage(),
                    "dailyChange", summary.getDailyChange(),
                    "dailyChangePercentage", summary.getDailyChangePercentage(),
                    "calculatedAt", LocalDateTime.now()
            );
            
            return PortfolioResponse.success(performanceData, "Portfolio performance calculated successfully");
            
        } catch (Exception e) {
            log.error("Error calculating portfolio performance for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Failed to calculate portfolio performance: " + e.getMessage());
        }
    }
    
    /**
     * Get market data for list of symbols with caching
     */
    private Map<String, MarketData> getMarketDataForSymbols(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Map.of();
        }
        
        try {
            // First, try to get cached data
            Map<String, MarketData> cachedData = cacheManager.getCachedMarketDataBatch(symbols);
            
            // Find symbols that need fresh data
            List<String> symbolsNeedingUpdate = symbols.stream()
                    .filter(symbol -> !cachedData.containsKey(symbol) || 
                            !isMarketDataFresh(cachedData.get(symbol)))
                    .toList();
            
            if (symbolsNeedingUpdate.isEmpty()) {
                log.debug("All market data found in cache for {} symbols", symbols.size());
                return cachedData;
            }
            
            log.debug("Fetching fresh market data for {} symbols", symbolsNeedingUpdate.size());
            
            // Fetch fresh data for missing symbols
            Map<String, MarketData> freshData = fetchFreshMarketData(symbolsNeedingUpdate);
            
            // Cache the fresh data
            if (!freshData.isEmpty()) {
                cacheManager.cacheMarketDataBatch(freshData.values().stream().toList());
            }
            
            // Combine cached and fresh data
            Map<String, MarketData> combinedData = cachedData.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            combinedData.putAll(freshData);
            
            return combinedData;
            
        } catch (Exception e) {
            log.error("Error getting market data for symbols: {}", e.getMessage(), e);
            return Map.of();
        }
    }
    
    /**
     * Fetch fresh market data from API
     */
    private Map<String, MarketData> fetchFreshMarketData(List<String> symbols) {
        try {
            return finnhubClient.getStockQuotes(symbols)
                    .collectMap(MarketData::getSymbol)
                    .timeout(java.time.Duration.ofSeconds(marketDataTimeoutSeconds))
                    .onErrorResume(throwable -> {
                        log.error("Error fetching market data: {}", throwable.getMessage());
                        return Mono.just(Map.of());
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error in fetchFreshMarketData: {}", e.getMessage(), e);
            return Map.of();
        }
    }
    
    /**
     * Refresh market data for specific symbols
     */
    private void refreshMarketDataForSymbols(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return;
        }
        
        try {
            // Invalidate cache for these symbols
            cacheManager.invalidateSymbols(symbols);
            
            // Trigger immediate update
            marketDataScheduler.updateSymbols(symbols);
            
        } catch (Exception e) {
            log.error("Error refreshing market data for symbols: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Check if market data is fresh enough
     */
    private boolean isMarketDataFresh(MarketData marketData) {
        if (marketData == null || marketData.getCacheTimestamp() == null) {
            return false;
        }
        
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(performanceCacheTtlMinutes);
        return marketData.getCacheTimestamp().isAfter(cutoff);
    }
    
    /**
     * Check if any market is currently open
     */
    private boolean isAnyMarketOpen(Map<String, MarketData> marketDataMap) {
        return marketDataMap.values().stream()
                .anyMatch(data -> data.getIsMarketOpen() != null && data.getIsMarketOpen());
    }
    
    /**
     * Calculate overall data freshness score
     */
    private BigDecimal calculateDataFreshness(Map<String, MarketData> marketDataMap) {
        if (marketDataMap.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        long freshDataCount = marketDataMap.values().stream()
                .filter(this::isMarketDataFresh)
                .count();
        
        return BigDecimal.valueOf(freshDataCount)
                .divide(BigDecimal.valueOf(marketDataMap.size()), 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Create empty portfolio summary
     */
    private PortfolioSummary createEmptyPortfolioSummary(Long userId) {
        return PortfolioSummary.builder()
                .userId(userId)
                .totalInvestment(BigDecimal.ZERO)
                .totalValue(BigDecimal.ZERO)
                .totalGainLoss(BigDecimal.ZERO)
                .totalGainLossPercentage(BigDecimal.ZERO)
                .dailyChange(BigDecimal.ZERO)
                .dailyChangePercentage(BigDecimal.ZERO)
                .totalPositions(0)
                .positionsWithCurrentData(0)
                .stockPerformances(List.of())
                .lastUpdated(LocalDateTime.now())
                .isMarketOpen(false)
                .dataFreshness(BigDecimal.valueOf(100))
                .build();
    }
    
    /**
     * Calculate additional portfolio metrics
     */
    private Map<String, Object> calculateAdditionalMetrics(Long userId, PortfolioSummary summary) {
        try {
            // Get portfolio statistics
            PortfolioResponse<Map<String, Object>> statsResponse = stockHoldingService.getPortfolioStatistics(userId);
            Map<String, Object> stats = statsResponse.isSuccess() ? statsResponse.getData() : Map.of();
            
            // Calculate additional metrics
            BigDecimal averageGainLoss = summary.getTotalPositions() > 0 ? 
                    summary.getTotalGainLoss().divide(BigDecimal.valueOf(summary.getTotalPositions()), 2, java.math.RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;
            
            int winningPositions = (int) summary.getStockPerformances().stream()
                    .filter(perf -> perf.getTotalGainLoss() != null && perf.getTotalGainLoss().compareTo(BigDecimal.ZERO) > 0)
                    .count();
            
            int losingPositions = (int) summary.getStockPerformances().stream()
                    .filter(perf -> perf.getTotalGainLoss() != null && perf.getTotalGainLoss().compareTo(BigDecimal.ZERO) < 0)
                    .count();
            
            return Map.of(
                    "averageGainLossPerPosition", averageGainLoss,
                    "winningPositions", winningPositions,
                    "losingPositions", losingPositions,
                    "winRate", summary.getTotalPositions() > 0 ? 
                            BigDecimal.valueOf(winningPositions).divide(BigDecimal.valueOf(summary.getTotalPositions()), 4, java.math.RoundingMode.HALF_UP) : 
                            BigDecimal.ZERO,
                    "portfolioStatistics", stats
            );
            
        } catch (Exception e) {
            log.error("Error calculating additional metrics: {}", e.getMessage(), e);
            return Map.of();
        }
    }
    
    /**
     * Get portfolio health check
     */
    public PortfolioResponse<Map<String, Object>> getPortfolioHealthCheck(Long userId) {
        try {
            log.info("Performing portfolio health check for user {}", userId);
            
            // Check API connectivity
            boolean apiConnected = finnhubClient.checkApiConnectivity().block();
            
            // Get cache statistics
            MarketDataCacheManager.CacheStatistics cacheStats = cacheManager.getCacheStatistics();
            
            // Get scheduler statistics
            MarketDataScheduler.SchedulerStatistics schedulerStats = marketDataScheduler.getSchedulerStatistics();
            
            // Get error handler statistics
            FinnhubErrorHandler.ErrorStatistics errorStats = errorHandler.getErrorStatistics();
            
            Map<String, Object> healthData = Map.of(
                    "apiConnectivity", apiConnected,
                    "cacheStatistics", cacheStats,
                    "schedulerStatistics", schedulerStats,
                    "errorStatistics", errorStats,
                    "lastHealthCheck", LocalDateTime.now()
            );
            
            return PortfolioResponse.success(healthData, "Portfolio health check completed");
            
        } catch (Exception e) {
            log.error("Error performing portfolio health check for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Portfolio health check failed: " + e.getMessage());
        }
    }
}