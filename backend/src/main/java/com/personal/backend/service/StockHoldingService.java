package com.personal.backend.service;

import com.personal.backend.dto.PortfolioResponse;
import com.personal.backend.dto.StockRequest;
import com.personal.backend.util.ValidationResult;
import com.personal.backend.model.CurrentStockPrice;
import com.personal.backend.model.StockTicker;
import com.personal.backend.repository.StockTickerRepository;
import com.personal.backend.repository.StockTickerRepository;
import com.personal.backend.repository.StockTransactionRepository;
import com.personal.backend.model.StockTransaction;
import com.personal.backend.util.FinancialCalculationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.backend.dto.MarketData;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockHoldingService {
    
    private final StockTickerRepository stockRepository;
    private final StockTransactionRepository transactionRepository;
    private final FinancialValidator financialValidator;
    private final YahooFinanceService yahooFinanceService;
    private final StockPriceCacheService stockPriceCacheService;
    
    @Value("${portfolio.max.positions.per.user:100}")
    private int maxPositionsPerUser;
    
    @Value("${portfolio.max.investment.per.position:1000000}")
    private BigDecimal maxInvestmentPerPosition;
    
    /**
     * Add a new stock holding to user's portfolio
     */
    public PortfolioResponse<StockTicker> addStockHolding(Long userId, StockRequest request) {
        try {
            log.info("Adding stock holding for user {}: {}", userId, request.getSymbol());
            
            // Validate user ID
            if (userId == null || userId <= 0) {
                return PortfolioResponse.validationError("Valid user ID is required");
            }
            
            // Normalize and validate request
            request.normalize();
            ValidationResult validation = financialValidator.validateStockRequest(request);
            if (!validation.isValid()) {
                return PortfolioResponse.validationError("Invalid stock request", validation.getErrors());
            }
            
            // Check if user already has this stock
            if (stockRepository.existsByUserIdAndSymbol(userId, request.getSymbol())) {
                return PortfolioResponse.duplicateError("Stock position for " + request.getSymbol());
            }
            
            // Check portfolio limits
            long currentPositions = stockRepository.countByUserId(userId);
            if (currentPositions >= maxPositionsPerUser) {
                return PortfolioResponse.limitExceededError("Maximum positions limit (" + maxPositionsPerUser + ")");
            }
            
            // Check investment limit
            if (request.getTotalInvestment().compareTo(maxInvestmentPerPosition) > 0) {
                return PortfolioResponse.limitExceededError("Maximum investment per position ($" + maxInvestmentPerPosition + ")");
            }
            
            // Create and save stock ticker
            StockTicker stockTicker = StockTicker.builder()
                    .symbol(request.getSymbol())
                    .purchasePrice(request.getPurchasePrice())
                    .quantity(request.getQuantity())
                    .userId(userId)
                    .notes(request.getNotes())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            StockTicker savedStock = stockRepository.save(stockTicker);
            
            log.info("Successfully added stock holding: {} for user {}", savedStock.getSymbol(), userId);
            
            Map<String, Object> metadata = Map.of(
                    "totalInvestment", savedStock.getTotalInvestment(),
                    "totalPositions", currentPositions + 1,
                    "warnings", validation.getWarnings() != null ? validation.getWarnings() : List.of()
            );
            
            return PortfolioResponse.success(savedStock, "Stock holding added successfully", metadata);
            
        } catch (Exception e) {
            log.error("Error adding stock holding for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Failed to add stock holding: " + e.getMessage());
        }
    }
    
    /**
     * Update an existing stock holding
     */
    public PortfolioResponse<StockTicker> updateStockHolding(Long userId, String symbol, StockRequest request) {
        try {
            log.info("Updating stock holding for user {}: {}", userId, symbol);
            
            // Validate inputs
            if (userId == null || userId <= 0) {
                return PortfolioResponse.validationError("Valid user ID is required");
            }
            
            if (symbol == null || symbol.trim().isEmpty()) {
                return PortfolioResponse.validationError("Stock symbol is required");
            }
            
            // Find existing stock
            Optional<StockTicker> existingStock = stockRepository.findByUserIdAndSymbol(userId, symbol.toUpperCase());
            if (existingStock.isEmpty()) {
                return PortfolioResponse.notFound("Stock holding for " + symbol);
            }
            
            // Validate request
            request.normalize();
            ValidationResult validation = financialValidator.validateStockRequest(request);
            if (!validation.isValid()) {
                return PortfolioResponse.validationError("Invalid stock request", validation.getErrors());
            }
            
            // Check investment limit
            if (request.getTotalInvestment().compareTo(maxInvestmentPerPosition) > 0) {
                return PortfolioResponse.limitExceededError("Maximum investment per position ($" + maxInvestmentPerPosition + ")");
            }
            
            // Update stock ticker
            StockTicker stockTicker = existingStock.get();
            BigDecimal oldInvestment = stockTicker.getTotalInvestment();
            
            stockTicker.setPurchasePrice(request.getPurchasePrice());
            stockTicker.setQuantity(request.getQuantity());
            stockTicker.setNotes(request.getNotes());
            stockTicker.setUpdatedAt(LocalDateTime.now());
            
            StockTicker updatedStock = stockRepository.save(stockTicker);
            
            log.info("Successfully updated stock holding: {} for user {}", updatedStock.getSymbol(), userId);
            
            Map<String, Object> metadata = Map.of(
                    "oldInvestment", oldInvestment,
                    "newInvestment", updatedStock.getTotalInvestment(),
                    "investmentChange", updatedStock.getTotalInvestment().subtract(oldInvestment),
                    "warnings", validation.getWarnings() != null ? validation.getWarnings() : List.of()
            );
            
            return PortfolioResponse.success(updatedStock, "Stock holding updated successfully", metadata);
            
        } catch (Exception e) {
            log.error("Error updating stock holding for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Failed to update stock holding: " + e.getMessage());
        }
    }
    
    /**
     * Remove a stock holding from user's portfolio
     */
    public PortfolioResponse<Boolean> removeStockHolding(Long userId, String symbol) {
        try {
            log.info("Removing stock holding for user {}: {}", userId, symbol);
            
            // Validate inputs
            if (userId == null || userId <= 0) {
                return PortfolioResponse.validationError("Valid user ID is required");
            }
            
            if (symbol == null || symbol.trim().isEmpty()) {
                return PortfolioResponse.validationError("Stock symbol is required");
            }
            
            // Find existing stock
            Optional<StockTicker> existingStock = stockRepository.findByUserIdAndSymbol(userId, symbol.toUpperCase());
            if (existingStock.isEmpty()) {
                return PortfolioResponse.notFound("Stock holding for " + symbol);
            }
            
            StockTicker stockTicker = existingStock.get();
            BigDecimal removedInvestment = stockTicker.getTotalInvestment();
            
            // Delete the stock
            stockRepository.delete(stockTicker);
            
            log.info("Successfully removed stock holding: {} for user {}", symbol, userId);
            
            Map<String, Object> metadata = Map.of(
                    "removedSymbol", symbol,
                    "removedInvestment", removedInvestment,
                    "remainingPositions", stockRepository.countByUserId(userId)
            );
            
            return PortfolioResponse.success(true, "Stock holding removed successfully", metadata);
            
        } catch (Exception e) {
            log.error("Error removing stock holding for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Failed to remove stock holding: " + e.getMessage());
        }
    }
    
    /**
     * Get all stock holdings for a user
     */
    @Transactional(readOnly = true)
    public PortfolioResponse<List<StockTicker>> getUserStockHoldings(Long userId) {
        try {
            log.debug("Retrieving stock holdings for user {}", userId);
            
            if (userId == null || userId <= 0) {
                return PortfolioResponse.validationError("Valid user ID is required");
            }
            
            List<StockTicker> holdings = stockRepository.findByUserIdOrderBySymbolAsc(userId);

            // Use the price cache service for smart price fetching
            List<String> symbols = holdings.stream()
                    .map(StockTicker::getSymbol)
                    .collect(Collectors.toList());
            
            Map<String, CurrentStockPrice> priceCache = stockPriceCacheService.getPricesWithRefresh(symbols);
            
            // Apply cached prices to holdings
            for (StockTicker ticker : holdings) {
                CurrentStockPrice cached = priceCache.get(ticker.getSymbol());
                if (cached != null && cached.getPrice() != null) {
                    ticker.setCurrentPrice(cached.getPrice());
                    ticker.setDailyChange(cached.getDailyChange());
                    ticker.setDailyChangePercentage(cached.getDailyChangePercent());
                    ticker.setLastPriceUpdate(cached.getFetchedAt());
                    ticker.setIsMarketOpen(cached.getMarketOpenWhenFetched());
                }
            }
            
            // Calculate portfolio statistics
            BigDecimal totalInvestment = holdings.stream()
                    .map(StockTicker::getTotalInvestment)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Map<String, Object> metadata = Map.of(
                    "totalPositions", holdings.size(),
                    "totalInvestment", totalInvestment,
                    "averagePositionSize", holdings.isEmpty() ? BigDecimal.ZERO : 
                            totalInvestment.divide(BigDecimal.valueOf(holdings.size()), 2, java.math.RoundingMode.HALF_UP),
                    "maxPositionsAllowed", maxPositionsPerUser
            );
            
            return PortfolioResponse.success(holdings, "Stock holdings retrieved successfully", metadata);
            
        } catch (Exception e) {
            log.error("Error retrieving stock holdings for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Failed to retrieve stock holdings: " + e.getMessage());
        }
    }
    
    /**
     * Get a specific stock holding
     */
    @Transactional(readOnly = true)
    public PortfolioResponse<StockTicker> getStockHolding(Long userId, String symbol) {
        try {
            log.debug("Retrieving stock holding for user {}: {}", userId, symbol);
            
            if (userId == null || userId <= 0) {
                return PortfolioResponse.validationError("Valid user ID is required");
            }
            
            if (symbol == null || symbol.trim().isEmpty()) {
                return PortfolioResponse.validationError("Stock symbol is required");
            }
            
            Optional<StockTicker> stock = stockRepository.findByUserIdAndSymbol(userId, symbol.toUpperCase());
            if (stock.isEmpty()) {
                return PortfolioResponse.notFound("Stock holding for " + symbol);
            }
            
            return PortfolioResponse.success(stock.get(), "Stock holding retrieved successfully");
            
        } catch (Exception e) {
            log.error("Error retrieving stock holding for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Failed to retrieve stock holding: " + e.getMessage());
        }
    }
    
    /**
     * Search stock holdings by symbol pattern
     */
    @Transactional(readOnly = true)
    public PortfolioResponse<List<StockTicker>> searchStockHoldings(Long userId, String searchTerm) {
        try {
            log.debug("Searching stock holdings for user {}: {}", userId, searchTerm);
            
            if (userId == null || userId <= 0) {
                return PortfolioResponse.validationError("Valid user ID is required");
            }
            
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return getUserStockHoldings(userId);
            }
            
            String cleanSearchTerm = searchTerm.trim();
            List<StockTicker> results;
            
            // Search by symbol first
            results = stockRepository.findByUserIdAndSymbolContainingIgnoreCaseOrderBySymbolAsc(userId, cleanSearchTerm);
            
            // If no results by symbol, search by notes
            if (results.isEmpty()) {
                results = stockRepository.findByUserIdAndNotesContainingIgnoreCaseOrderBySymbolAsc(userId, cleanSearchTerm);
            }
            
            Map<String, Object> metadata = Map.of(
                    "searchTerm", cleanSearchTerm,
                    "resultsCount", results.size()
            );
            
            return PortfolioResponse.success(results, "Search completed successfully", metadata);
            
        } catch (Exception e) {
            log.error("Error searching stock holdings for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Failed to search stock holdings: " + e.getMessage());
        }
    }
    
    /**
     * Get portfolio composition statistics
     */
    @Transactional(readOnly = true)
    public PortfolioResponse<Map<String, Object>> getPortfolioStatistics(Long userId) {
        try {
            log.debug("Calculating portfolio statistics for user {}", userId);
            
            if (userId == null || userId <= 0) {
                return PortfolioResponse.validationError("Valid user ID is required");
            }
            
            List<StockTicker> holdings = stockRepository.findByUserIdOrderBySymbolAsc(userId);
            
            if (holdings.isEmpty()) {
                Map<String, Object> emptyStats = Map.of(
                        "totalPositions", 0,
                        "totalInvestment", BigDecimal.ZERO,
                        "averagePositionSize", BigDecimal.ZERO,
                        "largestPosition", BigDecimal.ZERO,
                        "smallestPosition", BigDecimal.ZERO,
                        "symbols", List.of()
                );
                return PortfolioResponse.success(emptyStats, "Portfolio is empty");
            }
            
            // Calculate statistics
            BigDecimal totalInvestment = BigDecimal.ZERO;
            BigDecimal largestPosition = BigDecimal.ZERO;
            BigDecimal smallestPosition = null;
            
            for (StockTicker holding : holdings) {
                BigDecimal investment = holding.getTotalInvestment();
                totalInvestment = totalInvestment.add(investment);
                
                if (investment.compareTo(largestPosition) > 0) {
                    largestPosition = investment;
                }
                
                if (smallestPosition == null || investment.compareTo(smallestPosition) < 0) {
                    smallestPosition = investment;
                }
            }
            
            BigDecimal averagePositionSize = totalInvestment.divide(
                    BigDecimal.valueOf(holdings.size()), 2, java.math.RoundingMode.HALF_UP);
            
            List<String> symbols = holdings.stream()
                    .map(StockTicker::getSymbol)
                    .toList();
            
            Map<String, Object> statistics = Map.of(
                    "totalPositions", holdings.size(),
                    "totalInvestment", totalInvestment,
                    "averagePositionSize", averagePositionSize,
                    "largestPosition", largestPosition,
                    "smallestPosition", smallestPosition != null ? smallestPosition : BigDecimal.ZERO,
                    "symbols", symbols,
                    "maxPositionsAllowed", maxPositionsPerUser,
                    "positionsRemaining", maxPositionsPerUser - holdings.size()
            );
            
            return PortfolioResponse.success(statistics, "Portfolio statistics calculated successfully");
            
        } catch (Exception e) {
            log.error("Error calculating portfolio statistics for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Failed to calculate portfolio statistics: " + e.getMessage());
        }
    }
    
    /**
     * Get all unique symbols for a user (for market data fetching)
     */
    @Transactional(readOnly = true)
    public List<String> getUserSymbols(Long userId) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        return stockRepository.findDistinctSymbolsByUserId(userId);
    }
    
    /**
     * Check if user has reached maximum positions
     */
    @Transactional(readOnly = true)
    public boolean hasReachedMaxPositions(Long userId) {
        if (userId == null || userId <= 0) {
            return true;
        }
        return stockRepository.hasReachedMaxPositions(userId, maxPositionsPerUser);
    }
    
    /**
     * Bulk update stock holdings (for maintenance operations)
     */
    public PortfolioResponse<Integer> bulkUpdateStockHoldings(Long userId, List<StockRequest> requests) {
        try {
            log.info("Bulk updating {} stock holdings for user {}", requests.size(), userId);
            
            if (userId == null || userId <= 0) {
                return PortfolioResponse.validationError("Valid user ID is required");
            }
            
            if (requests == null || requests.isEmpty()) {
                return PortfolioResponse.validationError("No stock requests provided");
            }
            
            if (requests.size() > 50) {
                return PortfolioResponse.limitExceededError("Cannot update more than 50 positions at once");
            }
            
            int successCount = 0;
            List<String> errors = new java.util.ArrayList<>();
            
            for (StockRequest request : requests) {
                try {
                    PortfolioResponse<StockTicker> result = updateStockHolding(userId, request.getSymbol(), request);
                    if (result.isSuccess()) {
                        successCount++;
                    } else {
                        errors.add(request.getSymbol() + ": " + result.getError());
                    }
                } catch (Exception e) {
                    errors.add(request.getSymbol() + ": " + e.getMessage());
                }
            }
            
            String message = String.format("Updated %d of %d stock holdings", successCount, requests.size());
            Map<String, Object> metadata = Map.of(
                    "totalRequested", requests.size(),
                    "successCount", successCount,
                    "errorCount", errors.size(),
                    "errors", errors
            );
            
            if (successCount == 0) {
                return PortfolioResponse.error("Failed to update any stock holdings", metadata);
            }
            
            return PortfolioResponse.success(successCount, message, metadata);
            
        } catch (Exception e) {
            log.error("Error in bulk update for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Bulk update failed: " + e.getMessage());
        }
    }


    /**
     * Recalculate holdings based on transaction history
     */
    public PortfolioResponse<Integer> recalculateHoldings(Long userId) {
        try {
            log.info("Recalculating stock holdings for user {}", userId);
            
            if (userId == null || userId <= 0) {
                return PortfolioResponse.validationError("Valid user ID is required");
            }
            
            // 1. Fetch all transactions for user, ordered by date
            List<StockTransaction> allTransactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(userId); // Actually we need ASC for replay
            
            // We need to fetch all and sort in memory or use a different query method if DESC is default
            // Let's sort them by date ASC to replay history
            allTransactions.sort(java.util.Comparator.comparing(StockTransaction::getTransactionDate));
            
            // 2. Group by Symbol
            Map<String, List<StockTransaction>> txnsBySymbol = allTransactions.stream()
                    .collect(java.util.stream.Collectors.groupingBy(StockTransaction::getSymbol));
            
            int updatedCount = 0;
            
            // 3. Process each symbol
            for (Map.Entry<String, List<StockTransaction>> entry : txnsBySymbol.entrySet()) {
                String symbol = entry.getKey();
                List<StockTransaction> txns = entry.getValue();
                
                BigDecimal totalQuantity = BigDecimal.ZERO;
                BigDecimal totalCost = BigDecimal.ZERO;
                BigDecimal averagePrice = BigDecimal.ZERO;
                
                for (StockTransaction txn : txns) {
                    if (txn.getType() == StockTransaction.TransactionType.BUY) {
                        // BUY: Increase Quantity, Increase Cost
                        BigDecimal txnCost = txn.getTotalCost();
                        // Handle legacy data where totalCost might be null? Should be enforced but safety first
                        if (txnCost == null) {
                            txnCost = txn.getQuantity().multiply(txn.getPricePerShare());
                        }
                        
                        totalCost = totalCost.add(txnCost);
                        totalQuantity = totalQuantity.add(txn.getQuantity());
                        
                        // Recalculate Average Price
                        if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
                            averagePrice = totalCost.divide(totalQuantity, 4, java.math.RoundingMode.HALF_UP);
                        }
                        
                    } else if (txn.getType() == StockTransaction.TransactionType.SELL) {
                        // SELL: Reduce Quantity, Cost Basis reduces proportionally
                        // Cost removed = Qty Sold * Average Price
                        BigDecimal costRemoved = txn.getQuantity().multiply(averagePrice);
                        
                        totalCost = totalCost.subtract(costRemoved);
                        totalQuantity = totalQuantity.subtract(txn.getQuantity());
                        
                        // Average Price stays the same (mathematically) during a sell
                        // If fully sold, reset
                        if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                            totalQuantity = BigDecimal.ZERO;
                            totalCost = BigDecimal.ZERO;
                            averagePrice = BigDecimal.ZERO;
                        }
                    }
                }
                
                // 4. Update or Create StockTicker
                if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
                    Optional<StockTicker> existing = stockRepository.findByUserIdAndSymbol(userId, symbol);
                    StockTicker ticker;
                    
                    if (existing.isPresent()) {
                        ticker = existing.get();
                        // Only update if changed
                        if (ticker.getQuantity().compareTo(totalQuantity) != 0 || 
                            ticker.getPurchasePrice().compareTo(averagePrice) != 0) {
                            
                            log.info("Updating {} for user {}: Qty {} -> {}, AvgPrice {} -> {}", 
                                    symbol, userId, ticker.getQuantity(), totalQuantity, ticker.getPurchasePrice(), averagePrice);
                                    
                            ticker.setQuantity(totalQuantity);
                            ticker.setPurchasePrice(averagePrice);
                            ticker.setUpdatedAt(LocalDateTime.now());
                            stockRepository.save(ticker);
                            updatedCount++;
                        }
                    } else {
                        // Should be rare if we are rebuilding, but maybe a holding was deleted manually but txns remain
                        log.info("Restoring {} for user {}: Qty {}, AvgPrice {}", symbol, userId, totalQuantity, averagePrice);
                        ticker = StockTicker.builder()
                                .userId(userId)
                                .symbol(symbol)
                                .quantity(totalQuantity)
                                .purchasePrice(averagePrice)
                                .notes("Restored via Recalculation")
                                .build();
                        stockRepository.save(ticker);
                        updatedCount++;
                    }
                } else {
                    // Position is closed (0 qty). Ensure it's removed from Holdings if it exists
                    Optional<StockTicker> existing = stockRepository.findByUserIdAndSymbol(userId, symbol);
                    if (existing.isPresent()) {
                        log.info("Removing closed position {} for user {}", symbol, userId);
                        stockRepository.delete(existing.get());
                        updatedCount++;
                    }
                }
            }
            
            return PortfolioResponse.success(updatedCount, "Recalculated " + updatedCount + " holdings");
            
        } catch (Exception e) {
            log.error("Error recalculating holdings for user {}: {}", userId, e.getMessage(), e);
            return PortfolioResponse.error("Recalculation failed: " + e.getMessage());
        }
    }

    private void updatePriceIfStale(StockTicker ticker) {
        // Price is stale if null or older than 15 minutes
        boolean isStale = ticker.getCurrentPrice() == null || 
                          ticker.getLastPriceUpdate() == null || 
                          ticker.getLastPriceUpdate().isBefore(LocalDateTime.now().minusMinutes(15));

        if (isStale) {
            try {
                Optional<MarketData> marketDataOpt = yahooFinanceService.getMarketData(ticker.getSymbol());
                if (marketDataOpt.isPresent()) {
                    MarketData marketData = marketDataOpt.get();
                    ticker.setCurrentPrice(marketData.getCurrentPrice());
                    ticker.setDailyChange(marketData.getDailyChange());
                    ticker.setDailyChangePercentage(marketData.getDailyChangePercentage());
                    ticker.setLastPriceUpdate(LocalDateTime.now());
                    ticker.setIsMarketOpen(marketData.getIsMarketOpen());
                    stockRepository.save(ticker);
                    log.debug("Updated price for {} using Yahoo Finance: ${}", 
                             ticker.getSymbol(), marketData.getCurrentPrice());
                } else {
                    log.warn("Could not fetch price for {} from Yahoo Finance", ticker.getSymbol());
                }
            } catch (Exception e) {
                log.error("Failed to fetch price for {} after attempts: {}", 
                         ticker.getSymbol(), e.getMessage());
            }
        }
    }

    /**
     * Check if a stock's price is stale and needs updating.
     * Always fetches if no price exists. For existing prices, only refreshes during market hours.
     */
    private boolean isPriceStale(StockTicker ticker) {
        // Always fetch if we have no price at all - need initial data
        if (ticker.getCurrentPrice() == null || ticker.getLastPriceUpdate() == null) {
            return true;
        }
        
        // If market is closed and we have a price, don't refresh - prices won't change
        if (!isMarketOpen()) {
            return false;
        }
        
        // Market is open - refresh if price is older than 15 minutes
        return ticker.getLastPriceUpdate().isBefore(LocalDateTime.now().minusMinutes(15));
    }

    /**
     * Check if NYSE is currently open (Mon-Fri 9:30 AM - 4:00 PM Eastern).
     * Note: Does not account for holidays.
     */
    private boolean isMarketOpen() {
        ZonedDateTime nowET = ZonedDateTime.now(ZoneId.of("America/New_York"));
        DayOfWeek day = nowET.getDayOfWeek();
        LocalTime time = nowET.toLocalTime();
        
        // Market closed on weekends
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        
        // Market hours: 9:30 AM - 4:00 PM ET
        LocalTime marketOpen = LocalTime.of(9, 30);
        LocalTime marketClose = LocalTime.of(16, 0);
        
        return !time.isBefore(marketOpen) && time.isBefore(marketClose);
    }
}
