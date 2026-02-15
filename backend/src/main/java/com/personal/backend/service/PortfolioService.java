package com.personal.backend.service;

import com.personal.backend.dto.MarketData;
import com.personal.backend.dto.PortfolioHistoryPoint;
import com.personal.backend.dto.PortfolioResponse;
import com.personal.backend.dto.PortfolioSummary;
import com.personal.backend.dto.CompletePortfolioSummary;
import com.personal.backend.dto.StockPerformance;
import com.personal.backend.model.Account;
import com.personal.backend.model.AccountBalanceHistory;
import com.personal.backend.model.StockTicker;
import com.personal.backend.repository.AccountBalanceHistoryRepository;
import com.personal.backend.repository.AccountRepository;
import com.personal.backend.service.StockPriceCacheService;
import com.personal.backend.model.CurrentStockPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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
    private final YahooFinanceService yahooFinanceService;
    private final MarketDataCacheManager cacheManager;
    private final PerformanceCalculator performanceCalculator;
    private final MarketDataScheduler marketDataScheduler;
    private final AccountRepository accountRepository;
    private final AccountBalanceHistoryRepository accountBalanceHistoryRepository;
    private final com.personal.backend.repository.AccountTransactionRepository accountTransactionRepository;
    private final com.personal.backend.repository.StockTransactionRepository stockTransactionRepository;
    private final HistoricalPortfolioService historicalPortfolioService;
    private final StockPriceCacheService stockPriceCacheService;
    
    @Value("${portfolio.performance.cache.ttl.minutes:5}")
    private int performanceCacheTtlMinutes;
    
    @Value("${portfolio.market.data.timeout.seconds:30}")
    private int marketDataTimeoutSeconds;
    
    @Value("${portfolio.enable.real.time.updates:true}")
    private boolean enableRealTimeUpdates;
    
    /**
     * Get complete portfolio summary with real-time market data
     */
    @Transactional
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
            
            // Optimize: Construct market data directly from holdings (already hydrated)
            // This guarantees consistency between the table (Holdings) and the sidebar (Summary)
            Map<String, MarketData> marketDataMap = holdings.stream()
                .filter(h -> h.getCurrentPrice() != null)
                .collect(Collectors.toMap(
                    StockTicker::getSymbol,
                    h -> MarketData.builder()
                            .symbol(h.getSymbol())
                            .currentPrice(h.getCurrentPrice())
                            .dailyChange(h.getDailyChange())
                            .dailyChangePercentage(h.getDailyChangePercentage())
                            .timestamp(h.getLastPriceUpdate())
                            .isMarketOpen(h.getIsMarketOpen())
                            .build()
                ));
            
            // Calculate performance metrics
            PerformanceCalculator.PerformanceMetrics metrics = performanceCalculator.calculatePerformanceMetrics(holdings, marketDataMap);
            List<StockPerformance> stockPerformances = performanceCalculator.calculateBatchPerformance(holdings, marketDataMap);
            
            // Calculate Stock Portfolio Performance for various periods
            BigDecimal stockYtdGain = null;
            BigDecimal stockYtdGainPct = null;
            BigDecimal stockGain7d = null;
            BigDecimal stockGainPct7d = null;
            BigDecimal stockGain1m = null;
            BigDecimal stockGainPct1m = null;
            BigDecimal stockGain3m = null;
            BigDecimal stockGainPct3m = null;
            
            try {
                List<Account> stockAccounts = accountRepository.findByType(Account.AccountType.STOCK_PORTFOLIO);
                if (!stockAccounts.isEmpty()) {
                    Long stockAccountId = stockAccounts.get(0).getId();
                    BigDecimal currentTotalValue = metrics.currentValue();
                    LocalDate today = LocalDate.now();
                    
                    // Calculate Returns for different periods
                    
                    // YTD
                    LocalDate startOfYear = LocalDate.of(today.getYear(), 1, 1);
                    PeriodReturn ytdReturn = calculatePeriodReturn(userId, stockAccountId, startOfYear, today, currentTotalValue);
                    stockYtdGain = ytdReturn.gainLoss();
                    stockYtdGainPct = ytdReturn.percentage();
                    
                    // 7 Days
                    LocalDate start7d = today.minusDays(7);
                    PeriodReturn return7d = calculatePeriodReturn(userId, stockAccountId, start7d, today, currentTotalValue);
                    stockGain7d = return7d.gainLoss();
                    stockGainPct7d = return7d.percentage();
                    
                    // 1 Month
                    LocalDate start1m = today.minusMonths(1);
                    PeriodReturn return1m = calculatePeriodReturn(userId, stockAccountId, start1m, today, currentTotalValue);
                    stockGain1m = return1m.gainLoss();
                    stockGainPct1m = return1m.percentage();
                    
                    // 3 Months
                    LocalDate start3m = today.minusMonths(3);
                    PeriodReturn return3m = calculatePeriodReturn(userId, stockAccountId, start3m, today, currentTotalValue);
                    stockGain3m = return3m.gainLoss();
                    stockGainPct3m = return3m.percentage();
                }
            } catch (Exception e) {
                log.warn("Failed to calculate Stock Period Returns: {}", e.getMessage());
            }

            // Build portfolio summary
            PortfolioSummary summary = PortfolioSummary.builder()
                    .userId(userId)
                    .totalInvestment(metrics.totalInvestment())
                    .totalValue(metrics.currentValue())
                    .totalGainLoss(metrics.totalGainLoss())
                    .totalGainLossPercentage(metrics.totalGainLossPercentage())
                    .dailyChange(metrics.dailyChange())
                    .dailyChangePercentage(metrics.dailyChangePercentage())
                    .totalGainLossYTD(stockYtdGain)
                    .totalGainLossPercentageYTD(stockYtdGainPct)
                    .totalGainLoss7d(stockGain7d)
                    .totalGainLossPercentage7d(stockGainPct7d)
                    .totalGainLoss1m(stockGain1m)
                    .totalGainLossPercentage1m(stockGainPct1m)
                    .totalGainLoss3m(stockGain3m)
                    .totalGainLossPercentage3m(stockGainPct3m)
                    .totalPositions(metrics.totalPositions())
                    .positionsWithCurrentData(metrics.positionsWithData())
                    .stockPerformances(stockPerformances)
                    .lastUpdated(LocalDateTime.now())
                    .isMarketOpen(isAnyMarketOpen(marketDataMap))
                    .dataFreshness(calculateDataFreshness(marketDataMap))
                    .build();
            
            log.info("Portfolio summary generated for user {}: {} positions, ${} total value", 
                    userId, summary.getTotalPositions(), summary.getCurrentValue());
            
            // Sync the STOCK_PORTFOLIO account balance in the database
            updateStockAccountBalance(userId, summary.getCurrentValue());
            
            return PortfolioResponse.success(summary, "Portfolio summary generated successfully");
            
        } catch (Exception e) {
            log.error("Error generating portfolio summary for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Failed to generate portfolio summary: " + e.getMessage());
        }
    }
    
    @Transactional
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
     * Get complete portfolio summary including all account types
     */
    @Transactional(readOnly = true)
    public PortfolioResponse<CompletePortfolioSummary> getCompletePortfolioSummary(Long userId) {
        try {
            log.info("Generating complete portfolio summary for user {}", userId);
            
            // 1. Get stock portfolio summary (real-time)
            PortfolioResponse<PortfolioSummary> stockResponse = getPortfolioSummary(userId);
            if (!stockResponse.isSuccess()) {
                return PortfolioResponse.error("Failed to get stock portfolio: " + stockResponse.getError());
            }
            PortfolioSummary stockSummary = stockResponse.getData();
            
            // 2. Get all accounts from database
            List<Account> allAccounts = accountRepository.findAll();
            
            // 3. Separate Stock Portfolio account from others
            Account stockAccount = allAccounts.stream()
                    .filter(a -> Account.AccountType.STOCK_PORTFOLIO.equals(a.getType()))
                    .findFirst()
                    .orElse(null);
            
            List<Account> otherAccounts = allAccounts.stream()
                    .filter(a -> !Account.AccountType.STOCK_PORTFOLIO.equals(a.getType()))
                    .toList();
            
            // 4. Build StaticHoldings based on database accounts
            List<CompletePortfolioSummary.StaticHolding> staticHoldingsList = otherAccounts.stream()
                    .map(a -> CompletePortfolioSummary.StaticHolding.builder()
                            .name(a.getName())
                            .ticker(a.getName()) // Using name as ticker for manual accounts
                            .marketValue(a.getBalance())
                            .assetType(a.getType().name())
                            .notes(a.getNotes())
                            .build())
                    .toList();
            
            BigDecimal totalCash = otherAccounts.stream()
                    .filter(a -> Account.AccountType.CASH.equals(a.getType()))
                    .map(Account::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalRetirement = otherAccounts.stream()
                    .filter(a -> Account.AccountType.RETIREMENT.equals(a.getType()))
                    .map(Account::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalEducation = otherAccounts.stream()
                    .filter(a -> Account.AccountType.EDUCATION.equals(a.getType()))
                    .map(Account::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalOther = otherAccounts.stream()
                    .filter(a -> Account.AccountType.OTHER.equals(a.getType()))
                    .map(Account::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalStaticValue = otherAccounts.stream()
                    .map(Account::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            CompletePortfolioSummary.StaticHoldings staticHoldings = CompletePortfolioSummary.StaticHoldings.builder()
                    .totalCash(totalCash)
                    .totalRetirement(totalRetirement)
                    .totalOtherPortfolios(totalEducation.add(totalOther))
                    .totalStaticValue(totalStaticValue)
                    .asOfDate(LocalDateTime.now())
                    .holdings(staticHoldingsList)
                    .build();
            
            // 5. Calculate overall totals using the snapshot + change logic for stock account
            // Current Value = (Latest Stock Snapshot Balance) + (Today's Daily Change)
            // Note: If we have real-time data, stockSummary.getTotalValue() is already accurate.
            // But if we want to follow exactly "snapshot + daily change":
            BigDecimal stockLiveValue = stockSummary.getTotalValue(); 
            
            BigDecimal totalPortfolioValue = stockLiveValue.add(totalStaticValue);
            

            // Calculate YTD Performance
            LocalDate startOfYear = LocalDate.of(LocalDate.now().getYear(), 1, 1);
            LocalDate today = LocalDate.now();
            
            // 5a. Get history for all accounts in the YTD range
            List<AccountBalanceHistory> allHistory = accountBalanceHistoryRepository
                    .findByDateBetweenOrderByDateAsc(startOfYear, today);
            
            // 5b. Group by date and sum to get Total Portfolio Value for each day
            // Also need to track WHICH accounts contributed to each day to ensure completeness
            Map<LocalDate, List<AccountBalanceHistory>> historyByDate = allHistory.stream()
                    .collect(Collectors.groupingBy(AccountBalanceHistory::getDate));
            
            // Check if we have active stock accounts
            boolean hasStockAccount = stockAccount != null;
            Long stockAccountId = hasStockAccount ? stockAccount.getId() : null;

            // 5c. Find the first day where Total Value > 0 AND (if stock account exists, it is present)
            BigDecimal startOfYearValue = historyByDate.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .filter(entry -> {
                        // Check if total > 0
                        BigDecimal dailyTotal = entry.getValue().stream()
                                .map(AccountBalanceHistory::getBalance)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        
                        if (dailyTotal.compareTo(BigDecimal.ZERO) <= 0) return false;
                        
                        // Check completeness: If we have a stock account, is it in this day's history?
                        if (hasStockAccount) {
                             boolean stockPresent = entry.getValue().stream()
                                     .anyMatch(h -> h.getAccountId().equals(stockAccountId));
                             if (!stockPresent) {
                                 // Skip this day (likely holiday/weekend where only cash was recorded)
                                 return false;
                             }
                        }
                        return true;
                    })
                    .map(entry -> entry.getValue().stream()
                            .map(AccountBalanceHistory::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add))
                    .findFirst()
                    .orElse(BigDecimal.ZERO);
            
            log.info("YTD Calculation: StartValue (Aggregated & Complete)={}, CurrentValue={}", startOfYearValue, totalPortfolioValue);
            
            BigDecimal ytdGainLoss = totalPortfolioValue.subtract(startOfYearValue);
            
            // Calculate percentage on start value if > 0, else 0
            BigDecimal ytdGainPercentage = startOfYearValue.compareTo(BigDecimal.ZERO) > 0
                    ? ytdGainLoss.divide(startOfYearValue, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            
            // 6. Assemble complete summary
            CompletePortfolioSummary detailedSummary = CompletePortfolioSummary.builder()
                    .stockPortfolio(stockSummary)
                    .staticHoldings(staticHoldings)
                    .totalPortfolioValue(totalPortfolioValue)
                    .totalCashValue(totalCash)
                    .totalStockValue(stockLiveValue)
                    .totalRetirementValue(totalRetirement)
                    .totalOtherInvestments(totalEducation.add(totalOther))
                    .totalGainLossYTD(ytdGainLoss)
                    .totalGainLossPercentageYTD(ytdGainPercentage)
                    .allocationByType(Map.of(
                            "Stocks", stockLiveValue,
                            "Cash", totalCash,
                            "Retirement", totalRetirement,
                            "Other", totalEducation.add(totalOther)
                    ))
                    .lastUpdated(LocalDateTime.now())
                    .currency("USD")
                    .notes(List.of(
                            "Calculated from " + otherAccounts.size() + " manual accounts and real-time stock holdings.",
                            "Last refreshed: " + LocalDateTime.now()
                    ))
                    .build();
            
            return PortfolioResponse.success(detailedSummary, "Complete portfolio summary generated successfully");
            
        } catch (Exception e) {
            log.error("Error generating complete portfolio summary for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Failed to generate complete portfolio summary: " + e.getMessage());
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
     * Get portfolio value history for a specific period
     */
    @Transactional(readOnly = true)
    public PortfolioResponse<List<PortfolioHistoryPoint>> getPortfolioHistory(Long userId, String period) {
        try {
            log.info("Fetching portfolio history for user {} over period {}", userId, period);
            
            // Use synchronous method directly - avoids reactive overhead and .block()
            List<PortfolioHistoryPoint> history = historicalPortfolioService.getReconstructedHistorySync(userId, period);
            
            return PortfolioResponse.success(history, "History retrieved successfully");
            
        } catch (Exception e) {
            log.error("Error fetching portfolio history: {}", e.getMessage(), e);
            return PortfolioResponse.error("Failed to fetch history: " + e.getMessage());
        }
    }

    /**
     * Get market data for list of symbols using the centralized StockPriceCacheService.
     * This ensures consistent caching and market-hours logic across the application.
     */
    private Map<String, MarketData> getMarketDataForSymbols(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Map.of();
        }
        
        try {
            // Use the smart caching service (DB-backed, market-aware)
            // This handles staleness checks and fetching internally
            Map<String, CurrentStockPrice> prices = stockPriceCacheService.getPricesWithRefresh(symbols);
            
            // Convert to MarketData objects expected by the rest of the system
            return prices.entrySet().stream()
                    .map(entry -> {
                        String symbol = entry.getKey();
                        CurrentStockPrice price = entry.getValue();
                        
                        return MarketData.builder()
                                .symbol(symbol)
                                .currentPrice(price.getPrice())
                                .dailyChange(price.getDailyChange())
                                .dailyChangePercentage(price.getDailyChangePercent())
                                .timestamp(price.getFetchedAt())
                                .cacheTimestamp(price.getFetchedAt())
                                .isMarketOpen(stockPriceCacheService.isMarketOpen())
                                .build();
                    })
                    .collect(Collectors.toMap(MarketData::getSymbol, Function.identity()));
            
        } catch (Exception e) {
            log.error("Error getting market data for symbols: {}", e.getMessage(), e);
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
            // Use centralized cache service to force refresh
            stockPriceCacheService.forceRefresh(symbols);
            
        } catch (Exception e) {
            log.error("Error refreshing market data for symbols: {}", e.getMessage(), e);
        }
    }
    

    
    /**
     * Check if any market is currently open
     */
    private boolean isAnyMarketOpen(Map<String, MarketData> marketDataMap) {
        return stockPriceCacheService.isMarketOpen();
    }
    
    /**
     * Check if market data is fresh enough
     */
    private boolean isMarketDataFresh(MarketData marketData) {
        if (marketData == null || marketData.getCacheTimestamp() == null) {
            return false;
        }
        
        // If market is closed, any cached data is fresh
        if (!stockPriceCacheService.isMarketOpen()) {
            return true;
        }
        
        // Market is open - check TTL
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(performanceCacheTtlMinutes);
        return marketData.getCacheTimestamp().isAfter(cutoff);
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
            
            // Check API connectivity (using Yahoo Finance)
            boolean apiConnected = true; // Yahoo Finance library doesn't have a simple health check, assume OK if initialized
            
            // Get cache statistics
            MarketDataCacheManager.CacheStatistics cacheStats = cacheManager.getCacheStatistics();
            
            // Get scheduler statistics
            MarketDataScheduler.SchedulerStatistics schedulerStats = marketDataScheduler.getSchedulerStatistics();
            
            Map<String, Object> healthData = Map.of(
                    "apiConnectivity", apiConnected,
                    "cacheStatistics", cacheStats,
                    "schedulerStatistics", schedulerStats,
                    "lastHealthCheck", LocalDateTime.now()
            );
            
            return PortfolioResponse.success(healthData, "Portfolio health check completed");
            
        } catch (Exception e) {
            log.error("Error performing portfolio health check for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Portfolio health check failed: " + e.getMessage());
        }
    }
    /**
     * Update the balance of the STOCK_PORTFOLIO account in the database
     */
    private void updateStockAccountBalance(Long userId, BigDecimal currentValue) {
        try {
            List<Account> accounts = accountRepository.findByType(Account.AccountType.STOCK_PORTFOLIO);
            if (!accounts.isEmpty()) {
                for (Account stockAccount : accounts) {
                    if (stockAccount.getBalance().compareTo(currentValue) != 0) {
                        log.info("Updating stock account balance in DB (ID: {}): ${} -> ${}", 
                                stockAccount.getId(), stockAccount.getBalance(), currentValue);
                        stockAccount.setBalance(currentValue);
                        stockAccount.setUpdatedAt(LocalDateTime.now());
                        accountRepository.save(stockAccount);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to sync stock account balance: {}", e.getMessage());
        }
    }

    /**
     * Get market indices (DOW, NASDAQ, S&P 500, Bitcoin, Gold)
     */
    public List<Map<String, Object>> getMarketIndices() {
        List<String> indexSymbols = List.of("^DJI", "^IXIC", "^GSPC", "BTC-USD", "GC=F");
        List<String> indexNames = List.of("DOW", "NASDAQ", "S&P 500", "Bitcoin", "Gold");
        
        try {
            Map<String, CurrentStockPrice> prices = stockPriceCacheService.getPricesWithRefresh(indexSymbols);
            
            return java.util.stream.IntStream.range(0, indexSymbols.size())
                    .mapToObj(i -> {
                        String symbol = indexSymbols.get(i);
                        String name = indexNames.get(i);
                        CurrentStockPrice price = prices.get(symbol);
                        
                        Map<String, Object> indexData = new java.util.HashMap<>();
                        indexData.put("symbol", symbol);
                        indexData.put("name", name);
                        
                        if (price != null) {
                            indexData.put("price", price.getPrice());
                            indexData.put("change", price.getDailyChange());
                            indexData.put("changePercent", price.getDailyChangePercent());
                        } else {
                            indexData.put("price", BigDecimal.ZERO);
                            indexData.put("change", BigDecimal.ZERO);
                            indexData.put("changePercent", BigDecimal.ZERO);
                        }
                        
                        return indexData;
                    })
                    .toList();
                    
        } catch (Exception e) {
            log.error("Error fetching market indices: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get stock chart data for a specific symbol and time period
     */
    public List<Map<String, Object>> getStockChartData(String symbol, String period) {
        try {
            // Special handling for 1D: Return intraday data (5-min intervals)
            if ("1D".equalsIgnoreCase(period)) {
                return yahooFinanceService.getIntradayChartData(symbol);
            }

            // Calculate date range based on period for daily data
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = switch (period.toUpperCase()) {
                case "1W" -> endDate.minusWeeks(1);
                case "1M" -> endDate.minusMonths(1);
                case "6M" -> endDate.minusMonths(6);
                case "1Y" -> endDate.minusYears(1);
                default -> endDate.minusMonths(1);
            };
            
            // Use Yahoo Finance service to get historical data
            List<com.personal.backend.model.StockDailyPrice> dailyPrices = 
                    yahooFinanceService.getHistoricalPrices(symbol, startDate, endDate);
            
            return dailyPrices.stream()
                    .sorted(Comparator.comparing(com.personal.backend.model.StockDailyPrice::getDate))
                    .map(price -> {
                        Map<String, Object> dataPoint = new java.util.HashMap<>();
                        dataPoint.put("date", price.getDate().toString());
                        dataPoint.put("price", price.getClosePrice());
                        return dataPoint;
                    })
                    .toList();
                    
        } catch (Exception e) {
            log.error("Error fetching stock chart data for {}: {}", symbol, e.getMessage(), e);
            return List.of();
        }
    }


    private record PeriodReturn(BigDecimal gainLoss, BigDecimal percentage) {}
    
    /**
     * Helper to calculate return for a specific period using Net Investment Logic
     * Formula: Gain = (Current - Start) - NetFlows
     *          Pct  = Gain / (Start + NetFlows)
     */
    private PeriodReturn calculatePeriodReturn(Long userId, Long stockAccountId, LocalDate startDate, LocalDate endDate, BigDecimal currentValue) {
        BigDecimal gainLoss = null;
        BigDecimal percentage = null;
        
        try {
            // 1. Find Baseline (Start Value)
            // We search for history records between startDate and endDate
            // The baseline is the first record in this range with value > 0
            List<AccountBalanceHistory> history = accountBalanceHistoryRepository
                    .findByAccountIdAndDateBetweenOrderByDateAsc(stockAccountId, startDate, endDate);
            
            AccountBalanceHistory baseline = history.stream()
                    .filter(h -> h.getBalance().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst()
                    .orElse(null);
            
            if (baseline != null) {
                BigDecimal startVal = baseline.getBalance();
                // If baseline date is after start date, we use that date for transaction filtering too?
                // Ideally yes, but using the requested window is safer to capture all relevant flows if data is sparse.
                // However, logically "Net Investment" only matters *after* the baseline is established.
                // Let's use the actual baseline date as the start of the flow calculation window to be precise.
                LocalDate flowStartDate = baseline.getDate();
                
                // 2. Calculate Net Flows (Buys - Sells)
                List<com.personal.backend.model.StockTransaction> transactions = stockTransactionRepository
                        .findByUserIdAndTransactionDateBetween(userId, flowStartDate, endDate);
                
                BigDecimal totalBuys = BigDecimal.ZERO;
                BigDecimal totalSells = BigDecimal.ZERO;
                
                for (com.personal.backend.model.StockTransaction txn : transactions) {
                    BigDecimal amount = txn.getTotalCost();
                    if (amount == null) {
                        amount = txn.getQuantity().multiply(txn.getPricePerShare());
                    }
                    
                    if (txn.getType() == com.personal.backend.model.StockTransaction.TransactionType.BUY) {
                        totalBuys = totalBuys.add(amount);
                    } else if (txn.getType() == com.personal.backend.model.StockTransaction.TransactionType.SELL) {
                        totalSells = totalSells.add(amount);
                    }
                }
                
                BigDecimal netFlow = totalBuys.subtract(totalSells);
                
                // 3. Calculate Gain and Percentage
                // Adjusted Gain = (Current - Start) - NetFlow
                gainLoss = currentValue.subtract(startVal).subtract(netFlow);
                
                // Adjusted Baseline = Start + NetFlow
                BigDecimal adjustedBaseline = startVal.add(netFlow);
                
                if (adjustedBaseline.compareTo(BigDecimal.ZERO) > 0) {
                     percentage = gainLoss.divide(adjustedBaseline, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                }
            }
        } catch (Exception e) {
            log.warn("Error calculating return for period {} to {}: {}", startDate, endDate, e.getMessage());
        }
        
        return new PeriodReturn(gainLoss, percentage);
    }
}