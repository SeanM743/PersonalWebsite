package com.personal.backend.service;

import com.personal.backend.dto.MarketData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Intelligent caching layer for stock price data with TTL management and performance optimization
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataCacheManager {
    
    private final CacheManager cacheManager;
    
    @Value("${portfolio.cache.ttl.minutes:5}")
    private int cacheTtlMinutes;
    
    @Value("${portfolio.cache.market.hours.ttl.minutes:1}")
    private int marketHoursTtlMinutes;
    
    @Value("${portfolio.cache.after.hours.ttl.minutes:15}")
    private int afterHoursTtlMinutes;
    
    @Value("${portfolio.cache.warmup.enabled:true}")
    private boolean cacheWarmupEnabled;
    
    @Value("${portfolio.cache.max.entries:1000}")
    private int maxCacheEntries;
    
    // Cache statistics
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicInteger cacheSize = new AtomicInteger(0);
    
    // Frequently accessed symbols tracking
    private final Map<String, AtomicInteger> symbolAccessCounts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastAccessTimes = new ConcurrentHashMap<>();
    
    // Cache names
    private static final String MARKET_DATA_CACHE = "marketData";
    private static final String COMPANY_PROFILE_CACHE = "companyProfiles";
    private static final String MARKET_STATUS_CACHE = "marketStatus";
    
    /**
     * Get market data from cache or return null if not cached/expired
     */
    public Optional<MarketData> getCachedMarketData(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String normalizedSymbol = symbol.toUpperCase().trim();
        recordSymbolAccess(normalizedSymbol);
        
        Cache cache = cacheManager.getCache(MARKET_DATA_CACHE);
        if (cache == null) {
            log.warn("Market data cache not available");
            return Optional.empty();
        }
        
        Cache.ValueWrapper wrapper = cache.get(normalizedSymbol);
        if (wrapper != null) {
            MarketData cachedData = (MarketData) wrapper.get();
            if (cachedData != null && isCacheDataFresh(cachedData)) {
                cacheHits.incrementAndGet();
                log.debug("Cache hit for symbol: {}", normalizedSymbol);
                return Optional.of(cachedData);
            } else {
                // Remove stale data
                cache.evict(normalizedSymbol);
                log.debug("Evicted stale cache data for symbol: {}", normalizedSymbol);
            }
        }
        
        cacheMisses.incrementAndGet();
        log.debug("Cache miss for symbol: {}", normalizedSymbol);
        return Optional.empty();
    }
    
    /**
     * Cache market data with intelligent TTL based on market hours
     */
    public void cacheMarketData(MarketData marketData) {
        if (marketData == null || marketData.getSymbol() == null) {
            return;
        }
        
        String symbol = marketData.getSymbol().toUpperCase().trim();
        
        Cache cache = cacheManager.getCache(MARKET_DATA_CACHE);
        if (cache == null) {
            log.warn("Market data cache not available");
            return;
        }
        
        // Add cache metadata
        MarketData cacheableData = marketData.toBuilder()
                .cacheTimestamp(LocalDateTime.now())
                .cacheTtlMinutes(calculateTtl(marketData))
                .build();
        
        cache.put(symbol, cacheableData);
        cacheSize.incrementAndGet();
        
        log.debug("Cached market data for symbol: {} with TTL: {} minutes", 
                symbol, cacheableData.getCacheTtlMinutes());
    }
    
    /**
     * Cache multiple market data entries efficiently
     */
    public void cacheMarketDataBatch(List<MarketData> marketDataList) {
        if (marketDataList == null || marketDataList.isEmpty()) {
            return;
        }
        
        Cache cache = cacheManager.getCache(MARKET_DATA_CACHE);
        if (cache == null) {
            log.warn("Market data cache not available");
            return;
        }
        
        LocalDateTime cacheTime = LocalDateTime.now();
        int cachedCount = 0;
        
        for (MarketData marketData : marketDataList) {
            if (marketData != null && marketData.getSymbol() != null) {
                String symbol = marketData.getSymbol().toUpperCase().trim();
                
                MarketData cacheableData = marketData.toBuilder()
                        .cacheTimestamp(cacheTime)
                        .cacheTtlMinutes(calculateTtl(marketData))
                        .build();
                
                cache.put(symbol, cacheableData);
                cachedCount++;
            }
        }
        
        cacheSize.addAndGet(cachedCount);
        log.info("Batch cached {} market data entries", cachedCount);
    }
    
    /**
     * Get multiple cached market data entries
     */
    public Map<String, MarketData> getCachedMarketDataBatch(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, MarketData> results = new HashMap<>();
        Cache cache = cacheManager.getCache(MARKET_DATA_CACHE);
        
        if (cache == null) {
            return results;
        }
        
        for (String symbol : symbols) {
            if (symbol != null && !symbol.trim().isEmpty()) {
                String normalizedSymbol = symbol.toUpperCase().trim();
                recordSymbolAccess(normalizedSymbol);
                
                Cache.ValueWrapper wrapper = cache.get(normalizedSymbol);
                if (wrapper != null) {
                    MarketData cachedData = (MarketData) wrapper.get();
                    if (cachedData != null && isCacheDataFresh(cachedData)) {
                        results.put(normalizedSymbol, cachedData);
                        cacheHits.incrementAndGet();
                    } else {
                        cache.evict(normalizedSymbol);
                    }
                } else {
                    cacheMisses.incrementAndGet();
                }
            }
        }
        
        log.debug("Batch cache lookup: {} hits out of {} symbols", results.size(), symbols.size());
        return results;
    }
    
    /**
     * Invalidate cache for specific symbol
     */
    public void invalidateSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return;
        }
        
        String normalizedSymbol = symbol.toUpperCase().trim();
        Cache cache = cacheManager.getCache(MARKET_DATA_CACHE);
        
        if (cache != null) {
            cache.evict(normalizedSymbol);
            cacheSize.decrementAndGet();
            log.debug("Invalidated cache for symbol: {}", normalizedSymbol);
        }
    }
    
    /**
     * Invalidate cache for multiple symbols
     */
    public void invalidateSymbols(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return;
        }
        
        Cache cache = cacheManager.getCache(MARKET_DATA_CACHE);
        if (cache == null) {
            return;
        }
        
        int invalidatedCount = 0;
        for (String symbol : symbols) {
            if (symbol != null && !symbol.trim().isEmpty()) {
                String normalizedSymbol = symbol.toUpperCase().trim();
                cache.evict(normalizedSymbol);
                invalidatedCount++;
            }
        }
        
        cacheSize.addAndGet(-invalidatedCount);
        log.info("Invalidated cache for {} symbols", invalidatedCount);
    }
    
    /**
     * Clear all market data cache
     */
    public void clearCache() {
        Cache cache = cacheManager.getCache(MARKET_DATA_CACHE);
        if (cache != null) {
            cache.clear();
            cacheSize.set(0);
            log.info("Cleared all market data cache");
        }
    }
    
    /**
     * Get frequently accessed symbols for cache warming
     */
    public List<String> getFrequentlyAccessedSymbols(int limit) {
        return symbolAccessCounts.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicInteger>comparingByValue(
                        (a, b) -> b.get() - a.get()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }
    
    /**
     * Warm cache with frequently accessed symbols
     */
    public void warmCache(List<String> symbols) {
        if (!cacheWarmupEnabled || symbols == null || symbols.isEmpty()) {
            return;
        }
        
        log.info("Starting cache warmup for {} symbols", symbols.size());
        
        // This would typically trigger background fetching of market data
        // Implementation depends on integration with FinnhubClient
        for (String symbol : symbols) {
            recordSymbolAccess(symbol);
        }
        
        log.info("Cache warmup completed");
    }
    
    /**
     * Get cache statistics
     */
    public CacheStatistics getCacheStatistics() {
        long totalRequests = cacheHits.get() + cacheMisses.get();
        double hitRate = totalRequests > 0 ? (double) cacheHits.get() / totalRequests : 0.0;
        
        return new CacheStatistics(
                cacheHits.get(),
                cacheMisses.get(),
                hitRate,
                cacheSize.get(),
                maxCacheEntries,
                symbolAccessCounts.size(),
                getTopSymbols(10)
        );
    }
    
    /**
     * Scheduled cache maintenance
     * DISABLED: Commented out to prevent any automatic background tasks
     */
    // @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void performCacheMaintenance() {
        cleanupStaleEntries();
        cleanupAccessTracking();
        logCacheStatistics();
    }
    
    /**
     * Calculate TTL based on market hours and data freshness requirements
     */
    private int calculateTtl(MarketData marketData) {
        if (marketData.getIsMarketOpen() != null && marketData.getIsMarketOpen()) {
            return marketHoursTtlMinutes;
        } else {
            return afterHoursTtlMinutes;
        }
    }
    
    /**
     * Check if cached data is still fresh
     */
    private boolean isCacheDataFresh(MarketData cachedData) {
        if (cachedData.getCacheTimestamp() == null) {
            return false;
        }
        
        int ttlMinutes = cachedData.getCacheTtlMinutes() != null ? 
                cachedData.getCacheTtlMinutes() : cacheTtlMinutes;
        
        LocalDateTime expiryTime = cachedData.getCacheTimestamp().plusMinutes(ttlMinutes);
        return LocalDateTime.now().isBefore(expiryTime);
    }
    
    /**
     * Record symbol access for frequency tracking
     */
    private void recordSymbolAccess(String symbol) {
        symbolAccessCounts.computeIfAbsent(symbol, k -> new AtomicInteger(0)).incrementAndGet();
        lastAccessTimes.put(symbol, LocalDateTime.now());
    }
    
    /**
     * Clean up stale cache entries
     */
    private void cleanupStaleEntries() {
        Cache cache = cacheManager.getCache(MARKET_DATA_CACHE);
        if (cache == null) {
            return;
        }
        
        // This is a simplified cleanup - actual implementation would depend on cache provider
        log.debug("Performing cache cleanup");
    }
    
    /**
     * Clean up old access tracking data
     */
    private void cleanupAccessTracking() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        
        lastAccessTimes.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        symbolAccessCounts.entrySet().removeIf(entry -> !lastAccessTimes.containsKey(entry.getKey()));
        
        log.debug("Cleaned up access tracking data");
    }
    
    /**
     * Get top accessed symbols
     */
    private List<String> getTopSymbols(int limit) {
        return symbolAccessCounts.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicInteger>comparingByValue(
                        (a, b) -> b.get() - a.get()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }
    
    /**
     * Log cache statistics periodically
     */
    private void logCacheStatistics() {
        CacheStatistics stats = getCacheStatistics();
        log.info("Cache Statistics - Hits: {}, Misses: {}, Hit Rate: {:.2f}%, Size: {}/{}", 
                stats.hits(), stats.misses(), stats.hitRate() * 100, 
                stats.currentSize(), stats.maxSize());
    }
    
    /**
     * Cache statistics record
     */
    public record CacheStatistics(
            long hits,
            long misses,
            double hitRate,
            int currentSize,
            int maxSize,
            int trackedSymbols,
            List<String> topSymbols
    ) {}
}