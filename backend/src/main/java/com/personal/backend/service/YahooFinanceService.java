package com.personal.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.personal.backend.dto.MarketData;
import com.personal.backend.model.StockDailyPrice;
import com.personal.backend.repository.StockDailyPriceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class YahooFinanceService {

    private final StockDailyPriceRepository dailyPriceRepository;
    private final WebClient webClient;
    private final PerformanceMetricsService performanceMetricsService;
    
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    private String cookie = null;
    private String crumb = null;

    @Autowired
    public YahooFinanceService(StockDailyPriceRepository dailyPriceRepository, WebClient.Builder webClientBuilder, 
                               PerformanceMetricsService performanceMetricsService) {
        this.dailyPriceRepository = dailyPriceRepository;
        this.performanceMetricsService = performanceMetricsService;
        
        // Configure HttpClient with increased header size to handle large Yahoo Cookies
        HttpClient httpClient = HttpClient.create()
            .httpResponseDecoder(spec -> spec.maxHeaderSize(32 * 1024)); // 32KB

        this.webClient = webClientBuilder
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader("User-Agent", USER_AGENT)
            .codecs(configurer -> {
                configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024);
            })
            .build();
    }
    
    private void ensureAuthenticated() {
        if (crumb != null && cookie != null) {
            return;
        }
        
        try {
            log.info("Authenticating with Yahoo Finance...");
            
            // Strategy 1: Try fc.yahoo.com (often returns 404/302 but sets cookies)
            cookie = fetchCookie("https://fc.yahoo.com");
            
            // Strategy 2: Fallback to quote page (mimic browser)
            if (cookie == null) {
                log.info("Strategy 1 failed, trying Strategy 2: finance.yahoo.com/quote/AAPL");
                cookie = fetchCookie("https://finance.yahoo.com/quote/AAPL");
            }

            if (cookie != null) {
                log.info("Successfully obtained cookie. Fetching crumb...");
                
                // Step 2: Get Crumb
                String crumbUrl = "https://query1.finance.yahoo.com/v1/test/getcrumb";
                String fetchedCrumb = webClient.get()
                        .uri(crumbUrl)
                        .header("Cookie", cookie)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                
                if (fetchedCrumb != null && !fetchedCrumb.isEmpty()) {
                    crumb = fetchedCrumb.trim();
                    log.info("Obtained crumb: {}", crumb);
                } else {
                    log.error("Failed to obtain crumb");
                }
            } else {
                log.error("Failed to obtain cookie from both strategies");
            }
            
        } catch (Exception e) {
            log.error("Error during Yahoo authentication: {}", e.getMessage());
        }
    }
    
    private String fetchCookie(String url) {
        try {
            return webClient.get()
                    .uri(url)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .exchangeToMono(response -> {
                        // We care about headers, not status (fc.yahoo.com often 404s but sends cookie)
                        if (response.headers().asHttpHeaders().containsKey("Set-Cookie")) {
                            List<String> cookies = response.headers().asHttpHeaders().get("Set-Cookie");
                            if (cookies != null && !cookies.isEmpty()) {
                                String combined = String.join("; ", cookies);
                                log.info("Got {} cookies from {} (Status: {})", cookies.size(), url, response.statusCode());
                                return reactor.core.publisher.Mono.just(combined);
                            }
                        }
                        return response.releaseBody().then(reactor.core.publisher.Mono.empty());
                    })
                    .block();
        } catch (Exception e) {
            log.warn("Failed to fetch cookie from {}: {}", url, e.getMessage());
            return null;
        }
    }
    
    /**
     * Fetch and persist historical prices for a symbol from startDate to endDate
     */
    public void fetchAndPersistHistoricalPrices(String symbol, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching historical prices for {} from {} to {}", symbol, startDate, endDate);
        ensureAuthenticated();
        fetchAndPersistHistoricalPricesWithRetry(symbol, startDate, endDate, 10);
    }

    private void fetchAndPersistHistoricalPricesWithRetry(String symbol, LocalDate startDate, LocalDate endDate, int retries) {
        try {
            // Add a delay to avoid rate limiting
            Thread.sleep(2000);
            
            long p1 = startDate.atStartOfDay(ZoneId.of("UTC")).toEpochSecond();
            long p2 = endDate.atStartOfDay(ZoneId.of("UTC")).plusDays(1).toEpochSecond();
            
            String url = String.format("https://query1.finance.yahoo.com/v8/finance/chart/%s?period1=%d&period2=%d&interval=1d&events=history", 
                symbol, p1, p2);
                
            if (crumb != null) {
                url += "&crumb=" + crumb;
            }
            
            log.info("Requesting Yahoo v8 API: {}", url);
            
            var request = webClient.get().uri(url);
            if (cookie != null) {
                request.header("Cookie", cookie);
            }
            
            JsonNode root = request
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
     * Ensure we have historical data for all symbols from Jan 1, 2026 to present.
     * Skips fetching if market is closed and we already have data up to the last trading day.
     */
    public void ensureHistoricalDataExists(List<String> symbols) {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.now();
        LocalDate lastTradingDay = getLastTradingDay();
        
        for (String symbol : symbols) {
            try {
                Optional<LocalDate> latestDate = dailyPriceRepository.findLatestDateForSymbol(symbol);
                
                if (latestDate.isEmpty()) {
                    // No data exists, fetch from Jan 1, 2026
                    log.info("No data exists for {}, fetching from {}", symbol, startDate);
                    fetchAndPersistHistoricalPrices(symbol, startDate, endDate);
                } else if (latestDate.get().isBefore(lastTradingDay)) {
                    // Data exists but is outdated (missing last trading day data)
                    LocalDate fetchFrom = latestDate.get().plusDays(1);
                    log.info("Updating {} from {} to {}", symbol, fetchFrom, endDate);
                    fetchAndPersistHistoricalPrices(symbol, fetchFrom, endDate);
                } else {
                    log.info("{} is up to date (latest: {}, last trading day: {})", symbol, latestDate.get(), lastTradingDay);
                }
                
                // Add a small delay between symbols to be respectful to Yahoo Finance
                Thread.sleep(200);
                
            } catch (Exception e) {
                log.error("Error ensuring data for {}: {}", symbol, e.getMessage());
            }
        }
    }
    
    /**
     * Get the last trading day (most recent weekday that the market was open).
     * Returns Friday if today is Saturday/Sunday, or yesterday if after market close on weekday.
     */
    public LocalDate getLastTradingDay() {
        ZonedDateTime nowET = ZonedDateTime.now(ZoneId.of("America/New_York"));
        DayOfWeek day = nowET.getDayOfWeek();
        LocalTime time = nowET.toLocalTime();
        LocalTime marketClose = LocalTime.of(16, 0);
        
        if (day == DayOfWeek.SATURDAY) {
            return nowET.toLocalDate().minusDays(1); // Friday
        } else if (day == DayOfWeek.SUNDAY) {
            return nowET.toLocalDate().minusDays(2); // Friday
        } else if (time.isAfter(marketClose)) {
            // After market close, today is the last trading day
            return nowET.toLocalDate();
        } else {
            // During or before market hours, last trading day is yesterday (or Friday if Monday)
            if (day == DayOfWeek.MONDAY) {
                return nowET.toLocalDate().minusDays(3); // Friday
            }
            return nowET.toLocalDate().minusDays(1);
        }
    }
    
    /**
     * Check if we should skip historical data fetching.
     * Returns true if market is closed and we have data up to the last trading day.
     */
    public boolean shouldSkipHistoricalFetch(String symbol) {
        LocalDate lastTradingDay = getLastTradingDay();
        Optional<LocalDate> latestDate = dailyPriceRepository.findLatestDateForSymbol(symbol);
        
        if (latestDate.isEmpty()) {
            return false; // No data, must fetch
        }
        
        // Skip if we have data up to or after the last trading day
        return !latestDate.get().isBefore(lastTradingDay);
    }

    /**
     * Get historical prices for a symbol between startDate and endDate.
     * Fetches from database, ensuring data is present first.
     */
    public List<StockDailyPrice> getHistoricalPrices(String symbol, LocalDate startDate, LocalDate endDate) {
        // Ensure we have the data
        ensureHistoricalDataExists(List.of(symbol));
        
        // Retrieve from database
        return dailyPriceRepository.findBySymbolAndDateBetweenOrderByDateAsc(symbol, startDate, endDate);
    }

    /**
     * Get real-time MarketData for a single symbol
     */
    public Optional<MarketData> getMarketData(String symbol) {
        return getMarketDataWithRetry(symbol, 3);
    }

    private Optional<MarketData> getMarketDataWithRetry(String symbol, int retries) {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        int statusCode = 200;
        
        try {
            ensureAuthenticated();
            
            // Add a small delay to avoid rate limiting
            Thread.sleep(500); 
            
            String url = String.format("https://query1.finance.yahoo.com/v7/finance/quote?symbols=%s", symbol);
            if (crumb != null) {
                url += "&crumb=" + crumb;
            }
            
            var request = webClient.get().uri(url);
            if (cookie != null) {
                request.header("Cookie", cookie);
            }
            
            JsonNode root = request
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            if (root == null) {
                return Optional.empty();
            }
            
            JsonNode quoteResponse = root.path("quoteResponse");
            if (quoteResponse.isMissingNode() || quoteResponse.path("result").isEmpty()) {
                return Optional.empty();
            }
            
            JsonNode result = quoteResponse.path("result").get(0);
            
            String name = result.has("shortName") ? result.path("shortName").asText() : 
                         (result.has("longName") ? result.path("longName").asText() : symbol);
            
            success = true;
            return Optional.of(MarketData.builder()
                .symbol(result.path("symbol").asText())
                .companyName(name)
                .currentPrice(BigDecimal.valueOf(result.path("regularMarketPrice").asDouble()))
                .dailyChange(BigDecimal.valueOf(result.path("regularMarketChange").asDouble()))
                .dailyChangePercentage(BigDecimal.valueOf(result.path("regularMarketChangePercent").asDouble()))
                .previousClose(BigDecimal.valueOf(result.path("regularMarketPreviousClose").asDouble()))
                .high(BigDecimal.valueOf(result.path("regularMarketDayHigh").asDouble()))
                .low(BigDecimal.valueOf(result.path("regularMarketDayLow").asDouble()))
                .currency(result.path("currency").asText("USD"))
                .exchange(result.path("exchange").asText(""))
                .dataSource("Yahoo Finance v7 Quote API")
                .timestamp(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .hasError(false)
                .build());
                
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                statusCode = 429;
                if (retries > 0) {
                    log.warn("Rate limited (429) for {}, retrying in 5 seconds... ({} retries left)", symbol, retries - 1);
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    return getMarketDataWithRetry(symbol, retries - 1);
                }
            } else if (e.getMessage() != null && e.getMessage().contains("401")) {
                statusCode = 401;
            } else if (e.getMessage() != null && e.getMessage().contains("403")) {
                statusCode = 403;
            } else {
                statusCode = 500;
            }
            log.error("Error fetching market data for {}: {}", symbol, e.getLocalizedMessage());
            return Optional.empty();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            performanceMetricsService.recordExternalApiCall(
                "yahoo_finance", "quote", java.time.Duration.ofMillis(duration), success, statusCode);
        }
    }

    /**
     * Get real-time MarketData for multiple symbols
     */
    /**
     * Get real-time MarketData for multiple symbols using batch API
     */
    public Map<String, MarketData> getBatchMarketData(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }
        
        long startTime = System.currentTimeMillis();
        boolean success = false;
        int statusCode = 200;
        
        try {
            ensureAuthenticated();
            
            String joinedSymbols = String.join(",", symbols);
            String url = String.format("https://query1.finance.yahoo.com/v7/finance/quote?symbols=%s", joinedSymbols);
            
            if (crumb != null) {
                url += "&crumb=" + crumb;
            }
            
            var request = webClient.get().uri(url);
            if (cookie != null) {
                request.header("Cookie", cookie);
            }
            
            JsonNode root = request
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            if (root == null || !root.has("quoteResponse") || root.get("quoteResponse").get("result").isNull()) {
                return Collections.emptyMap();
            }
            
            JsonNode results = root.path("quoteResponse").path("result");
            Map<String, MarketData> marketDataMap = new HashMap<>();
            
            for (JsonNode result : results) {
                String symbol = result.path("symbol").asText().toUpperCase();
                String name = result.has("shortName") ? result.path("shortName").asText() : 
                             (result.has("longName") ? result.path("longName").asText() : symbol);
                
                MarketData data = MarketData.builder()
                    .symbol(symbol)
                    .companyName(name)
                    .currentPrice(BigDecimal.valueOf(result.path("regularMarketPrice").asDouble()))
                    .dailyChange(BigDecimal.valueOf(result.path("regularMarketChange").asDouble()))
                    .dailyChangePercentage(BigDecimal.valueOf(result.path("regularMarketChangePercent").asDouble()))
                    .previousClose(BigDecimal.valueOf(result.path("regularMarketPreviousClose").asDouble()))
                    .high(BigDecimal.valueOf(result.path("regularMarketDayHigh").asDouble()))
                    .low(BigDecimal.valueOf(result.path("regularMarketDayLow").asDouble()))
                    .currency(result.has("currency") ? result.path("currency").asText() : "USD")
                    .exchange(result.has("exchange") ? result.path("exchange").asText() : "")
                    .dataSource("Yahoo Finance v7 Batch API")
                    .timestamp(LocalDateTime.now())
                    .lastUpdated(LocalDateTime.now())
                    .hasError(false)
                    .build();
                    
                marketDataMap.put(symbol, data);
            }
            
            success = true;
            return marketDataMap;
            
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            statusCode = e.getStatusCode().value();
            if (statusCode == 401 || statusCode == 403) {
                log.warn("Yahoo API returned 401/403 Unauthorized. Clearing credentials to force re-auth.");
                this.cookie = null;
                this.crumb = null;
            }
            log.error("Failed to fetch batch market data: {}", e.getMessage());
            return Collections.emptyMap();
        } catch (Exception e) {
            statusCode = 500;
            log.error("Failed to fetch batch market data: {}", e.getMessage());
            return Collections.emptyMap();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            performanceMetricsService.recordExternalApiCall(
                "yahoo_finance", "batch_quote", java.time.Duration.ofMillis(duration), success, statusCode);
        }
    }
    private final Map<String, CachedIntradayData> intradayCache = new java.util.concurrent.ConcurrentHashMap<>();

    private record CachedIntradayData(List<Map<String, Object>> data, LocalDateTime fetchedAt) {}

    /**
     * Get intraday chart data (5m interval) for the most recent trading day.
     * Caches the result for 15 minutes to avoid rate limits.
     */
    public List<Map<String, Object>> getIntradayChartData(String symbol) {
        // Check cache
        CachedIntradayData cached = intradayCache.get(symbol);
        if (cached != null && cached.fetchedAt.isAfter(LocalDateTime.now().minusMinutes(15))) {
            return cached.data;
        }

        long startTime = System.currentTimeMillis();
        boolean success = false;
        int statusCode = 200;

        try {
            ensureAuthenticated();
            
            // range=1d, interval=5m gets high granular data for the requested day
            // If market is closed, it gets the last trading session automatically
            String url = String.format("https://query1.finance.yahoo.com/v8/finance/chart/%s?range=1d&interval=5m", symbol);
            
            if (crumb != null) {
                url += "&crumb=" + crumb;
            }
            
            var request = webClient.get().uri(url);
            if (cookie != null) {
                request.header("Cookie", cookie);
            }
            
            JsonNode root = request
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            
            if (root == null || !root.has("chart") || root.get("chart").get("result").isNull()) {
                return List.of();
            }
            
            JsonNode result = root.get("chart").get("result").get(0);
            JsonNode timestamps = result.get("timestamp");
            JsonNode indicators = result.get("indicators").get("quote").get(0).get("close");
            
            if (timestamps == null || indicators == null || timestamps.size() == 0) {
                return List.of();
            }
            
            List<Map<String, Object>> chartData = new ArrayList<>();
            for (int i = 0; i < timestamps.size(); i++) {
                if (indicators.get(i).isNull()) continue;
                
                long timestamp = timestamps.get(i).asLong();
                LocalDateTime dateTime = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
                BigDecimal price = BigDecimal.valueOf(indicators.get(i).asDouble());
                
                Map<String, Object> point = new HashMap<>();
                point.put("date", dateTime.toString()); // ISO-8601 string
                point.put("price", price);
                chartData.add(point);
            }
            
            // Update cache
            intradayCache.put(symbol, new CachedIntradayData(chartData, LocalDateTime.now()));
            
            success = true;
            return chartData;
            
        } catch (Exception e) {
            statusCode = 500;
            log.error("Error fetching intraday data for {}: {}", symbol, e.getMessage());
            return List.of();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            performanceMetricsService.recordExternalApiCall(
                "yahoo_finance", "intraday_chart", java.time.Duration.ofMillis(duration), success, statusCode);
        }
    }
}
