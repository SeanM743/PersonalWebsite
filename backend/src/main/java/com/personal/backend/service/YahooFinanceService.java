package com.personal.backend.service;

import com.personal.backend.dto.MarketData;
import com.personal.backend.model.StockDailyPrice;
import com.personal.backend.repository.StockDailyPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class YahooFinanceService {
    
    private final StockDailyPriceRepository dailyPriceRepository;
    
    /**
     * Fetch and persist historical prices for a symbol from startDate to endDate
     */
    public void fetchAndPersistHistoricalPrices(String symbol, LocalDate startDate, LocalDate endDate) {
        try {
            log.info("Fetching historical prices for {} from {} to {}", symbol, startDate, endDate);
            
            Calendar from = Calendar.getInstance();
            from.setTime(Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            
            Calendar to = Calendar.getInstance();
            to.setTime(Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            
            Stock stock = YahooFinance.get(symbol, from, to, Interval.DAILY);
            
            if (stock == null) {
                log.warn("Stock not found for symbol: {}", symbol);
                return;
            }
            
            List<HistoricalQuote> history = stock.getHistory();
            
            if (history == null || history.isEmpty()) {
                log.warn("No historical data found for {}", symbol);
                return;
            }
            
            List<StockDailyPrice> prices = new ArrayList<>();
            for (HistoricalQuote quote : history) {
                if (quote.getDate() == null || quote.getClose() == null) {
                    continue;
                }
                
                LocalDate date = quote.getDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
                BigDecimal closePrice = quote.getClose();
                
                // Check if already exists
                if (dailyPriceRepository.findBySymbolAndDate(symbol, date).isEmpty()) {
                    prices.add(StockDailyPrice.builder()
                        .symbol(symbol)
                        .date(date)
                        .closePrice(closePrice)
                        .build());
                }
            }
            
            if (!prices.isEmpty()) {
                dailyPriceRepository.saveAll(prices);
                log.info("Persisted {} price points for {}", prices.size(), symbol);
            } else {
                log.info("All prices already exist for {}", symbol);
            }
            
        } catch (Exception e) {
            log.error("Failed to fetch historical prices for {}: {}", symbol, e.getMessage(), e);
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
        try {
            Stock stock = YahooFinance.get(symbol);
            if (stock == null || stock.getQuote() == null) {
                return Optional.empty();
            }
            return Optional.of(mapToMarketData(stock));
        } catch (Exception e) {
            log.error("Error fetching market data for {}: {}", symbol, e.getMessage());
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
        try {
            Map<String, Stock> stocks = YahooFinance.get(symbols.toArray(new String[0]));
            return stocks.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().getQuote() != null)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> mapToMarketData(e.getValue())
                ));
        } catch (Exception e) {
            log.error("Error fetching batch market data: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private MarketData mapToMarketData(Stock stock) {
        StockQuote quote = stock.getQuote();
        return MarketData.builder()
            .symbol(stock.getSymbol())
            .currentPrice(quote.getPrice())
            .previousClose(quote.getPreviousClose())
            .dailyChange(quote.getChange())
            .dailyChangePercentage(quote.getChangeInPercent())
            .high(quote.getDayHigh())
            .low(quote.getDayLow())
            .open(quote.getOpen())
            .volume(quote.getVolume())
            .timestamp(LocalDateTime.now())
            .isMarketOpen(quote.getPrice() != null) // Simplification
            .currency(stock.getCurrency())
            .exchange(stock.getStockExchange())
            .dataSource("Yahoo Finance")
            .lastUpdated(LocalDateTime.now())
            .hasError(false)
            .build();
    }
}
