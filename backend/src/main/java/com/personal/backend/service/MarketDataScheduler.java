package com.personal.backend.service;

import com.personal.backend.dto.MarketData;
import com.personal.backend.repository.StockTickerRepository;
import com.personal.backend.util.MarketHoursUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Scheduled market data updates with market-aware frequency adjustment
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataScheduler {
    
    private final YahooFinanceService yahooFinanceService;
    private final MarketDataCacheManager cacheManager;
    private final StockTickerRepository stockRepository;
    private final MarketHoursUtil marketHoursUtil;
    private final ThreadPoolTaskScheduler taskScheduler;
    
    @Value("${portfolio.market.data.refresh.interval.seconds:300}")
    private int defaultRefreshIntervalSeconds;
    
    @Value("${portfolio.market.data.batch.size:20}")
    private int batchSize;
    
    @Value("${portfolio.market.data.max.concurrent.updates:5}")
    private int maxConcurrentUpdates;
    
    @Value("${portfolio.market.data.scheduler.enabled:true}")
    private boolean schedulerEnabled;
    
    // Scheduler state
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicInteger activeUpdates = new AtomicInteger(0);
    private final AtomicLong lastUpdateTime = new AtomicLong(0);
    private final AtomicLong totalUpdates = new AtomicLong(0);
    private final AtomicInteger failedUpdates = new AtomicInteger(0);
    
    private ScheduledFuture<?> scheduledTask;
    
    @PostConstruct
    public void initialize() {
        if (schedulerEnabled) {
            startScheduler();
            log.info("Market data scheduler initialized and started");
        } else {
            log.info("Market data scheduler is disabled");
        }
    }
    
    @PreDestroy
    public void shutdown() {
        stopScheduler();
        log.info("Market data scheduler shutdown completed");
    }
    
    /**
     * Start the dynamic scheduler
     */
    public void startScheduler() {
        if (isRunning.compareAndSet(false, true)) {
            scheduleNextUpdate();
            log.info("Market data scheduler started");
        }
    }
    
    /**
     * Stop the scheduler
     */
    public void stopScheduler() {
        if (isRunning.compareAndSet(true, false)) {
            if (scheduledTask != null && !scheduledTask.isCancelled()) {
                scheduledTask.cancel(false);
            }
            log.info("Market data scheduler stopped");
        }
    }
    
    /**
     * Schedule next update based on market hours
     */
    private void scheduleNextUpdate() {
        if (!isRunning.get()) {
            return;
        }
        
        Duration interval = marketHoursUtil.getOptimalRefreshInterval();
        
        scheduledTask = taskScheduler.schedule(
                this::performScheduledUpdate,
                java.time.Instant.now().plus(interval)
        );
        
        log.debug("Next market data update scheduled in {} minutes", interval.toMinutes());
    }
    
    /**
     * Perform scheduled market data update
     */
    private void performScheduledUpdate() {
        try {
            if (!isRunning.get()) {
                return;
            }
            
            log.debug("Starting scheduled market data update");
            
            // Get all unique symbols that need updates
            List<String> symbols = getSymbolsNeedingUpdate();
            
            if (symbols.isEmpty()) {
                log.debug("No symbols need market data updates");
                scheduleNextUpdate();
                return;
            }
            
            // Check if we're within concurrent update limits
            if (activeUpdates.get() >= maxConcurrentUpdates) {
                log.warn("Maximum concurrent updates reached, skipping this cycle");
                scheduleNextUpdate();
                return;
            }
            
            activeUpdates.incrementAndGet();
            
            // Perform batch updates
            updateMarketDataBatch(symbols)
                    .doFinally(signalType -> {
                        activeUpdates.decrementAndGet();
                        lastUpdateTime.set(System.currentTimeMillis());
                        scheduleNextUpdate();
                    })
                    .subscribe(
                            count -> {
                                totalUpdates.addAndGet(count);
                                log.info("Completed scheduled market data update for {} symbols", count);
                            },
                            error -> {
                                failedUpdates.incrementAndGet();
                                log.error("Scheduled market data update failed: {}", error.getMessage());
                            }
                    );
            
        } catch (Exception e) {
            log.error("Error in scheduled market data update: {}", e.getMessage(), e);
            activeUpdates.decrementAndGet();
            scheduleNextUpdate();
        }
    }
    
    /**
     * Update market data for batch of symbols
     */
    private Mono<Integer> updateMarketDataBatch(List<String> symbols) {
        return Mono.fromCallable(() -> {
            log.debug("Processing batch of {} symbols", symbols.size());
            
            Map<String, MarketData> marketDataMap = yahooFinanceService.getBatchMarketData(symbols);
            List<MarketData> marketDataList = new ArrayList<>(marketDataMap.values());
            
            // Cache the results
            cacheManager.cacheMarketDataBatch(marketDataList);
            
            // Count successful updates
            int successCount = (int) marketDataList.stream()
                    .filter(data -> !data.hasError())
                    .count();
            
            log.debug("Batch update completed: {}/{} successful", 
                    successCount, symbols.size());
            
            return successCount;
        }).onErrorResume(error -> {
            log.error("Batch update failed for {} symbols: {}", 
                    symbols.size(), error.getMessage());
            return Mono.just(0);
        });
    }
    
    /**
     * Get symbols that need market data updates
     */
    private List<String> getSymbolsNeedingUpdate() {
        // Get all active symbols (symbols with recent activity)
        LocalDateTime recentCutoff = LocalDateTime.now().minusDays(7);
        List<String> activeSymbols = stockRepository.findActiveSymbols(recentCutoff);
        
        // During market hours, update all active symbols
        if (marketHoursUtil.isUSMarketOpen()) {
            return activeSymbols;
        }
        
        // During off-hours, prioritize frequently accessed symbols
        List<String> frequentSymbols = cacheManager.getFrequentlyAccessedSymbols(50);
        
        // Combine and deduplicate
        return activeSymbols.stream()
                .filter(symbol -> frequentSymbols.contains(symbol) || 
                        shouldUpdateDuringOffHours(symbol))
                .distinct()
                .limit(100) // Limit to prevent excessive API usage
                .toList();
    }
    
    /**
     * Determine if symbol should be updated during off-hours
     */
    private boolean shouldUpdateDuringOffHours(String symbol) {
        // Check if cached data is stale
        return cacheManager.getCachedMarketData(symbol)
                .map(data -> !isCacheDataFresh(data))
                .orElse(true);
    }
    
    /**
     * Check if cached data is fresh enough for off-hours
     */
    private boolean isCacheDataFresh(MarketData data) {
        if (data.getCacheTimestamp() == null) {
            return false;
        }
        
        // During off-hours, data is fresh for longer periods
        Duration maxAge = marketHoursUtil.isUSMarketOpen() ? 
                Duration.ofMinutes(5) : Duration.ofMinutes(30);
        
        return data.getCacheTimestamp().isAfter(
                LocalDateTime.now().minus(maxAge));
    }
    
    /**
     * Manual trigger for immediate market data update
     */
    public void triggerImmediateUpdate() {
        if (!isRunning.get()) {
            log.warn("Cannot trigger immediate update - scheduler is not running");
            return;
        }
        
        if (activeUpdates.get() >= maxConcurrentUpdates) {
            log.warn("Cannot trigger immediate update - maximum concurrent updates reached");
            return;
        }
        
        log.info("Triggering immediate market data update");
        
        List<String> symbols = stockRepository.findAllDistinctSymbols();
        if (symbols.isEmpty()) {
            log.info("No symbols to update");
            return;
        }
        
        activeUpdates.incrementAndGet();
        
        updateMarketDataBatch(symbols)
                .doFinally(signalType -> activeUpdates.decrementAndGet())
                .subscribe(
                        count -> log.info("Immediate update completed for {} symbols", count),
                        error -> log.error("Immediate update failed: {}", error.getMessage())
                );
    }
    
    /**
     * Update market data for specific symbols
     */
    public void updateSymbols(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return;
        }
        
        if (activeUpdates.get() >= maxConcurrentUpdates) {
            log.warn("Cannot update symbols - maximum concurrent updates reached");
            return;
        }
        
        log.info("Updating market data for {} specific symbols", symbols.size());
        
        activeUpdates.incrementAndGet();
        
        updateMarketDataBatch(symbols)
                .doFinally(signalType -> activeUpdates.decrementAndGet())
                .subscribe(
                        count -> log.info("Symbol-specific update completed for {} symbols", count),
                        error -> log.error("Symbol-specific update failed: {}", error.getMessage())
                );
    }
    
    /**
     * Fixed-rate fallback scheduler (runs every 5 minutes as backup)
     * DISABLED: Commented out to prevent automatic API calls
     */
    // @Scheduled(fixedRate = 300000) // 5 minutes
    public void fallbackScheduledUpdate() {
        if (!schedulerEnabled || !isRunning.get()) {
            return;
        }
        
        // Only run if main scheduler hasn't run recently
        long timeSinceLastUpdate = System.currentTimeMillis() - lastUpdateTime.get();
        if (timeSinceLastUpdate > 600000) { // 10 minutes
            log.warn("Main scheduler appears inactive, running fallback update");
            performScheduledUpdate();
        }
    }
    
    /**
     * Warm cache during market open
     * DISABLED: Commented out to prevent automatic API calls
     */
    // @Scheduled(cron = "0 25 9 * * MON-FRI", zone = "America/New_York") // 9:25 AM ET on weekdays
    public void warmCacheBeforeMarketOpen() {
        if (!schedulerEnabled) {
            return;
        }
        
        if (!marketHoursUtil.isTradingDay(java.time.LocalDate.now())) {
            log.debug("Skipping cache warmup - not a trading day");
            return;
        }
        
        log.info("Warming cache before market open");
        
        List<String> frequentSymbols = cacheManager.getFrequentlyAccessedSymbols(100);
        if (!frequentSymbols.isEmpty()) {
            updateSymbols(frequentSymbols);
        }
    }
    
    /**
     * Get scheduler statistics
     */
    public SchedulerStatistics getSchedulerStatistics() {
        return new SchedulerStatistics(
                isRunning.get(),
                activeUpdates.get(),
                totalUpdates.get(),
                failedUpdates.get(),
                lastUpdateTime.get(),
                marketHoursUtil.getCurrentMarketSession(),
                marketHoursUtil.getOptimalRefreshInterval()
        );
    }
    
    /**
     * Reset scheduler statistics
     */
    public void resetStatistics() {
        totalUpdates.set(0);
        failedUpdates.set(0);
        lastUpdateTime.set(0);
        log.info("Scheduler statistics reset");
    }
    
    /**
     * Scheduler statistics record
     */
    public record SchedulerStatistics(
            boolean isRunning,
            int activeUpdates,
            long totalUpdates,
            int failedUpdates,
            long lastUpdateTime,
            MarketHoursUtil.MarketSession currentSession,
            Duration nextUpdateInterval
    ) {}
}