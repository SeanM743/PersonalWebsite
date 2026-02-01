package com.personal.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.personal.backend.dto.MarketData;
import com.personal.backend.model.StockDailyPrice;
import com.personal.backend.repository.StockDailyPriceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class YahooFinanceService {

    private final StockDailyPriceRepository dailyPriceRepository;
    private final WebClient webClient;
    
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

    public YahooFinanceService(StockDailyPriceRepository dailyPriceRepository, WebClient.Builder webClientBuilder) {
        this.dailyPriceRepository = dailyPriceRepository;
        this.webClient = webClientBuilder
            .defaultHeader("User-Agent", USER_AGENT)
            .build();
    }
    
    /**
     * Fetch and persist historical prices for a symbol from startDate to endDate
     */
    public void fetchAndPersistHistoricalPrices(String symbol, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching historical prices for {} from {} to {}", symbol, startDate, endDate);
        fetchAndPersistHistoricalPricesWithRetry(symbol, startDate, endDate, 10);
    }

    private void fetchAndPersistHistoricalPricesWithRetry(String symbol, LocalDate startDate, LocalDate endDate, int retries) {
        try {
            // Add a delay to avoid rate limiting
            Thread.sleep(2000);
            
            long p1 = startDate.atStartOfDay(ZoneId.of("UTC")).toEpochSecond();
            long p2 = endDate.atStartOfDay(ZoneId.of("UTC")).plusDays(1).toEpochSecond();
            
            String url = String.format("https://query1.finance.yahoo.com/v8/finance/chart/%s?period1=%d&period2=%d&interval=1d", 
                symbol, p1, p2);
            
            log.info("Requesting Yahoo v8 API: {}", url);
            
            JsonNode root = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            if (root == null || !root.has("chart") || root.get("chart").get("result").isNull()) {
                log.warn("No result found for symbol: {}", symbol);
                return;
            }
            
            JsonNode result = root.get("chart").get("result").get(0);
            JsonNode timestamps = result.get("timestamp");
            JsonNode indicators = result.get("indicators").get("quote").get(0).get("close");
            
            if (timestamps == null || indicators == null || timestamps.size() == 0) {
                log.warn("No historical data points found for {}", symbol);
                return;
            }
            
            int persistedCount = 0;
            for (int i = 0; i < timestamps.size(); i++) {
                if (indicators.get(i).isNull()) continue;
                
                long timestamp = timestamps.get(i).asLong();
                LocalDate date = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
                BigDecimal closePrice = BigDecimal.valueOf(indicators.get(i).asDouble());
                
                // Check if already exists
                if (dailyPriceRepository.findBySymbolAndDate(symbol, date).isEmpty()) {
                    dailyPriceRepository.save(StockDailyPrice.builder()
                        .symbol(symbol)
                        .date(date)
                        .closePrice(closePrice)
                        .build());
                    persistedCount++;
                }
            }
            
            if (persistedCount > 0) {
                log.info("Persisted {} price points for {}", persistedCount, symbol);
            } else {
                log.info("All prices already exist for {}", symbol);
            }
            
        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null && message.contains("429") && retries > 0) {
                log.warn("Rate limited (429) for historical data of {}, retrying in 30 seconds... ({} retries left)", symbol, retries - 1);
                try { Thread.sleep(30000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                fetchAndPersistHistoricalPricesWithRetry(symbol, startDate, endDate, retries - 1);
            } else {
                log.error("Failed to fetch historical prices for {}: {}", symbol, message);
            }
        }
    }
    
    /**
     * Get closing price for a symbol on a specific date
     * If exact date not found, returns the most recent price before that date
     */
    public Optional<BigDecimal> getClosingPrice(String symbol, LocalDate date) {
        // Try exact date first
        Optional<StockDailyPrice> price = dailyPriceRepository.findBySymbolAndDate(symbol, date);
        if (price.isPresent()) {
            return Optional.of(price.get().getClosePrice());
        }
        
        // Fallback: get most recent price before this date (for weekends/holidays)
        LocalDate lookbackDate = date.minusDays(1);
        for (int i = 0; i < 7; i++) { // Look back up to 7 days
            price = dailyPriceRepository.findBySymbolAndDate(symbol, lookbackDate);
            if (price.isPresent()) {
                return Optional.of(price.get().getClosePrice());
            }
            lookbackDate = lookbackDate.minusDays(1);
        }
        
        return Optional.empty();
    }
    
    /**
     * Ensure we have historical data for all symbols from Jan 1, 2026 to present
     */
    public void ensureHistoricalDataExists(List<String> symbols) {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.now();
        
        for (String symbol : symbols) {
            try {
                Optional<LocalDate> latestDate = dailyPriceRepository.findLatestDateForSymbol(symbol);
                
                if (latestDate.isEmpty()) {
                    // No data exists, fetch from Jan 1, 2026
                    log.info("No data exists for {}, fetching from {}", symbol, startDate);
                    fetchAndPersistHistoricalPrices(symbol, startDate, endDate);
                } else if (latestDate.get().isBefore(endDate.minusDays(1))) {
                    // Data exists but is outdated, fetch missing dates
                    LocalDate fetchFrom = latestDate.get().plusDays(1);
                    log.info("Updating {} from {} to {}", symbol, fetchFrom, endDate);
                    fetchAndPersistHistoricalPrices(symbol, fetchFrom, endDate);
                } else {
                    log.info("{} is up to date (latest: {})", symbol, latestDate.get());
                }
                
                // Add a small delay between symbols to be respectful to Yahoo Finance
                Thread.sleep(200);
                
            } catch (Exception e) {
                log.error("Error ensuring data for {}: {}", symbol, e.getMessage());
            }
        }
    }

    /**
     * Get real-time MarketData for a single symbol
     */
    public Optional<MarketData> getMarketData(String symbol) {
        return getMarketDataWithRetry(symbol, 3);
    }

    private Optional<MarketData> getMarketDataWithRetry(String symbol, int retries) {
        try {
            // Add a small delay to avoid rate limiting
            Thread.sleep(500); 
            
            String url = String.format("https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=1d&range=1d", symbol);
            
            JsonNode root = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            if (root == null || !root.has("chart") || root.get("chart").get("result").isNull()) {
                return Optional.empty();
            }
            
            JsonNode result = root.get("chart").get("result").get(0);
            JsonNode meta = result.get("meta");
            
            return Optional.of(MarketData.builder()
                .symbol(symbol)
                .currentPrice(BigDecimal.valueOf(meta.get("regularMarketPrice").asDouble()))
                .previousClose(BigDecimal.valueOf(meta.get("chartPreviousClose").asDouble()))
                .high(BigDecimal.valueOf(meta.get("regularMarketDayHigh").asDouble()))
                .low(BigDecimal.valueOf(meta.get("regularMarketDayLow").asDouble()))
                .currency(meta.get("currency").asText())
                .exchange(meta.get("exchangeName").asText())
                .dataSource("Yahoo Finance v8 API")
                .timestamp(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .hasError(false)
                .build());
                
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("429") && retries > 0) {
                log.warn("Rate limited (429) for {}, retrying in 5 seconds... ({} retries left)", symbol, retries - 1);
                try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                return getMarketDataWithRetry(symbol, retries - 1);
            }
            log.error("Error fetching market data for {}: {}", symbol, e.getLocalizedMessage());
            return Optional.empty();
        }
    }

    /**
     * Get real-time MarketData for multiple symbols
     */
    public Map<String, MarketData> getBatchMarketData(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // Yahoo v8 chart API doesn't support batching as easily as v7 quote, 
        // so we'll fetch them individually for now, but we'll use parallel fetching in the future if needed.
        Map<String, MarketData> results = new HashMap<>();
        for (String symbol : symbols) {
            getMarketData(symbol).ifPresent(data -> results.put(symbol, data));
        }
        return results;
    }
}
