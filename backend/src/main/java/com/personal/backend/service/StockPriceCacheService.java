package com.personal.backend.service;

import com.personal.backend.dto.MarketData;
import com.personal.backend.model.CurrentStockPrice;
import com.personal.backend.repository.CurrentStockPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for managing the stock price cache.
 * Provides smart refresh logic based on market hours.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockPriceCacheService {
    
    private final CurrentStockPriceRepository priceRepository;
    private final YahooFinanceService yahooFinanceService;
    
    private static final ZoneId ET_ZONE = ZoneId.of("America/New_York");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);
    private static final int STALE_MINUTES = 15;
    
    /**
     * Get cached prices for a list of symbols, refreshing stale ones as needed.
     * Returns a map of symbol -> CurrentStockPrice
     */
    @Transactional
    public Map<String, CurrentStockPrice> getPricesWithRefresh(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // Get all cached prices
        List<CurrentStockPrice> cached = priceRepository.findBySymbolIn(symbols);
        Map<String, CurrentStockPrice> priceMap = cached.stream()
                .collect(Collectors.toMap(CurrentStockPrice::getSymbol, Function.identity()));
        
        // Find symbols that need refresh
        List<String> needsRefresh = symbols.stream()
                .filter(symbol -> needsPriceRefresh(priceMap.get(symbol)))
                .collect(Collectors.toList());
        
        if (!needsRefresh.isEmpty()) {
            log.info("Refreshing prices for {} symbols: {}", needsRefresh.size(), needsRefresh);
            refreshPricesParallel(needsRefresh, priceMap);
        }
        
        return priceMap;
    }

    /**
     * Force refresh prices for specific symbols (bypass cache)
     */
    @Transactional
    public void forceRefresh(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return;
        }
        
        log.info("Force refreshing prices for {} symbols", symbols.size());
        
        // Get any existing entities so we can update them instead of creating duplicates/errors
        List<CurrentStockPrice> cached = priceRepository.findBySymbolIn(symbols);
        Map<String, CurrentStockPrice> priceMap = cached.stream()
                .collect(Collectors.toMap(CurrentStockPrice::getSymbol, Function.identity()));
        
        refreshPricesParallel(symbols, priceMap);
    }
    
    /**
     * Determine if a cached price needs to be refreshed.
     */
    private boolean needsPriceRefresh(CurrentStockPrice cached) {
        // No cached price or name - always need to fetch
        if (cached == null || cached.getPrice() == null || cached.getCompanyName() == null) {
            return true;
        }
        
        LocalDateTime fetchedAt = cached.getFetchedAt();
        if (fetchedAt == null) {
            return true;
        }
        
        // Crypto logic: Always open, so always check 15-minute staleness
        if (isCrypto(cached.getSymbol())) {
            return fetchedAt.isBefore(LocalDateTime.now().minusMinutes(STALE_MINUTES));
        }
        
        // If market is currently open, refresh if older than 15 minutes
        if (isMarketOpen()) {
            return fetchedAt.isBefore(LocalDateTime.now().minusMinutes(STALE_MINUTES));
        }
        
        // Market is closed - if we have a price fetched AFTER the last market close, it's valid
        ZonedDateTime lastMarketClose = getLastMarketClose();
        ZonedDateTime fetchedZDT = fetchedAt.atZone(ZoneId.systemDefault());
        
        // If fetched after last market close, we have the valid closing price
        if (fetchedZDT.isAfter(lastMarketClose)) {
            log.debug("Price for {} is valid - fetched after last market close", cached.getSymbol());
            return false;
        }
        
        // Price is from before last market close - needs refresh
        return true;
    }
    
    private boolean isCrypto(String symbol) {
        return symbol != null && (symbol.endsWith("-USD") || symbol.equals("BTC-USD"));
    }
    
    /**
     * Get the datetime of the last market close (4 PM ET on last trading day).
     */
    private ZonedDateTime getLastMarketClose() {
        ZonedDateTime nowET = ZonedDateTime.now(ET_ZONE);
        DayOfWeek day = nowET.getDayOfWeek();
        LocalTime time = nowET.toLocalTime();
        
        // Determine the last trading day
        ZonedDateTime lastClose;
        if (day == DayOfWeek.SATURDAY) {
            // Last trading was Friday
            lastClose = nowET.minusDays(1).with(MARKET_CLOSE);
        } else if (day == DayOfWeek.SUNDAY) {
            // Last trading was Friday
            lastClose = nowET.minusDays(2).with(MARKET_CLOSE);
        } else if (day == DayOfWeek.MONDAY && time.isBefore(MARKET_OPEN)) {
            // Before Monday open - last trading was Friday
            lastClose = nowET.minusDays(3).with(MARKET_CLOSE);
        } else if (time.isBefore(MARKET_OPEN)) {
            // Before market open on a trading day - last close was yesterday
            lastClose = nowET.minusDays(1).with(MARKET_CLOSE);
        } else if (time.isAfter(MARKET_CLOSE)) {
            // After market close - last close was today
            lastClose = nowET.with(MARKET_CLOSE);
        } else {
            // During market hours - last close was yesterday
            lastClose = nowET.minusDays(1).with(MARKET_CLOSE);
        }
        
        return lastClose;
    }
    
    /**
     * Refresh prices in parallel for better performance.
     */
    private void refreshPricesParallel(List<String> symbols, Map<String, CurrentStockPrice> priceMap) {
        boolean marketOpen = isMarketOpen();
        
        try {
            Map<String, MarketData> batchData = yahooFinanceService.getBatchMarketData(symbols);
            
            for (Map.Entry<String, MarketData> entry : batchData.entrySet()) {
                String symbol = entry.getKey();
                MarketData data = entry.getValue();
                
                CurrentStockPrice price = priceMap.getOrDefault(symbol, 
                        CurrentStockPrice.builder().symbol(symbol).build());
                
                price.setPrice(data.getCurrentPrice());
                price.setCompanyName(data.getCompanyName());
                price.setDailyChange(data.getDailyChange());
                price.setDailyChangePercent(data.getDailyChangePercentage());
                price.setFetchedAt(LocalDateTime.now());
                price.setMarketOpenWhenFetched(marketOpen);
                
                priceRepository.save(price);
                priceMap.put(symbol, price);
                
                log.debug("Updated price cache for {}: ${}", symbol, data.getCurrentPrice());
            }
            
            // Log missing symbols?
            if (batchData.size() < symbols.size()) {
                log.warn("Batch fetch returned {} items, expected {}", batchData.size(), symbols.size());
            }
            
        } catch (Exception e) {
            log.error("Failed to refresh prices batch: {}", e.getMessage());
        }
    }
    
    /**
     * Update a single price in the cache (called after transactions, etc.)
     */
    @Transactional
    public void updatePrice(String symbol, MarketData data) {
        CurrentStockPrice price = priceRepository.findById(symbol)
                .orElse(CurrentStockPrice.builder().symbol(symbol).build());
        
        price.setPrice(data.getCurrentPrice());
        price.setCompanyName(data.getCompanyName());
        price.setDailyChange(data.getDailyChange());
        price.setDailyChangePercent(data.getDailyChangePercentage());
        price.setFetchedAt(LocalDateTime.now());
        price.setMarketOpenWhenFetched(isMarketOpen());
        
        priceRepository.save(price);
    }
    
    /**
     * Check if NYSE is currently open (Mon-Fri 9:30 AM - 4:00 PM Eastern).
     */
    public boolean isMarketOpen() {
        ZonedDateTime nowET = ZonedDateTime.now(ET_ZONE);
        DayOfWeek day = nowET.getDayOfWeek();
        LocalTime time = nowET.toLocalTime();
        
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        
        return !time.isBefore(MARKET_OPEN) && time.isBefore(MARKET_CLOSE);
    }
    
    /**
     * Get a single cached price (no refresh).
     */
    public Optional<CurrentStockPrice> getCachedPrice(String symbol) {
        return priceRepository.findById(symbol);
    }
}
