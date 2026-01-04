package com.personal.backend.service;

import com.personal.backend.dto.MarketData;
import com.personal.backend.external.FinnhubApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finnhub API client for real-time stock market data
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinnhubClient {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${portfolio.finnhub.api.key}")
    private String apiKey;
    
    @Value("${portfolio.finnhub.api.base.url:https://finnhub.io/api/v1}")
    private String baseUrl;
    
    @Value("${portfolio.finnhub.api.timeout:10000}")
    private int timeoutMs;
    
    @Value("${portfolio.finnhub.rate.limit.requests.per.minute:60}")
    private int rateLimitPerMinute;
    
    // Rate limiting tracking
    private final Map<String, Long> lastRequestTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();
    
    private WebClient webClient;
    
    /**
     * Initialize WebClient with proper configuration
     */
    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                    .baseUrl(baseUrl)
                    .defaultHeader("X-Finnhub-Token", apiKey)
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                    .build();
        }
        return webClient;
    }
    
    /**
     * Get real-time quote for a single stock symbol
     */
    public Mono<MarketData> getStockQuote(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Stock symbol cannot be null or empty"));
        }
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Finnhub API key not configured, returning empty market data for {}", symbol);
            return Mono.just(createEmptyMarketData(symbol));
        }
        
        String normalizedSymbol = symbol.toUpperCase().trim();
        
        return checkRateLimit(normalizedSymbol)
                .then(getWebClient()
                        .get()
                        .uri("/quote?symbol={symbol}", normalizedSymbol)
                        .retrieve()
                        .bodyToMono(FinnhubApiResponse.Quote.class)
                        .timeout(Duration.ofMillis(timeoutMs))
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                                .filter(this::isRetryableException))
                        .map(quote -> mapToMarketData(normalizedSymbol, quote))
                        .doOnSuccess(data -> log.debug("Successfully fetched quote for {}: ${}", 
                                normalizedSymbol, data.getCurrentPrice()))
                        .onErrorResume(throwable -> {
                            log.error("Error fetching quote for {}: {}", normalizedSymbol, throwable.getMessage());
                            return Mono.just(createErrorMarketData(normalizedSymbol, throwable.getMessage()));
                        }));
    }
    
    /**
     * Get real-time quotes for multiple stock symbols (batch request)
     */
    public Flux<MarketData> getStockQuotes(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Flux.empty();
        }
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Finnhub API key not configured, returning empty market data for {} symbols", symbols.size());
            return Flux.fromIterable(symbols)
                    .map(this::createEmptyMarketData);
        }
        
        // Process symbols in batches to respect rate limits
        int batchSize = Math.min(10, rateLimitPerMinute / 6); // Conservative batching
        
        return Flux.fromIterable(symbols)
                .distinct()
                .map(String::toUpperCase)
                .map(String::trim)
                .filter(symbol -> !symbol.isEmpty())
                .buffer(batchSize)
                .delayElements(Duration.ofSeconds(1)) // Delay between batches
                .flatMap(batch -> 
                    Flux.fromIterable(batch)
                            .flatMap(this::getStockQuote, 3) // Max 3 concurrent requests per batch
                            .collectList()
                            .flatMapMany(Flux::fromIterable)
                )
                .doOnComplete(() -> log.info("Completed batch quote fetch for {} symbols", symbols.size()));
    }
    
    /**
     * Get company profile information
     */
    public Mono<FinnhubApiResponse.CompanyProfile> getCompanyProfile(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Stock symbol cannot be null or empty"));
        }
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Mono.empty();
        }
        
        String normalizedSymbol = symbol.toUpperCase().trim();
        
        return checkRateLimit(normalizedSymbol)
                .then(getWebClient()
                        .get()
                        .uri("/stock/profile2?symbol={symbol}", normalizedSymbol)
                        .retrieve()
                        .bodyToMono(FinnhubApiResponse.CompanyProfile.class)
                        .timeout(Duration.ofMillis(timeoutMs))
                        .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
                                .filter(this::isRetryableException))
                        .doOnSuccess(profile -> log.debug("Successfully fetched company profile for {}", normalizedSymbol))
                        .onErrorResume(throwable -> {
                            log.error("Error fetching company profile for {}: {}", normalizedSymbol, throwable.getMessage());
                            return Mono.empty();
                        }));
    }
    
    /**
     * Get market status for US exchanges
     */
    public Mono<FinnhubApiResponse.MarketStatus> getMarketStatus() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Mono.empty();
        }
        
        return checkRateLimit("market-status")
                .then(getWebClient()
                        .get()
                        .uri("/stock/market-status?exchange=US")
                        .retrieve()
                        .bodyToMono(FinnhubApiResponse.MarketStatus.class)
                        .timeout(Duration.ofMillis(timeoutMs))
                        .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
                                .filter(this::isRetryableException))
                        .doOnSuccess(status -> log.debug("Successfully fetched market status: {}", status.getIsOpen()))
                        .onErrorResume(throwable -> {
                            log.error("Error fetching market status: {}", throwable.getMessage());
                            return Mono.empty();
                        }));
    }
    
    /**
     * Check API connectivity and validate API key
     */
    public Mono<Boolean> checkApiConnectivity() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Finnhub API key not configured");
            return Mono.just(false);
        }
        
        return getStockQuote("AAPL")
                .map(marketData -> !marketData.hasError())
                .onErrorReturn(false)
                .doOnNext(connected -> {
                    if (connected) {
                        log.info("Finnhub API connectivity verified");
                    } else {
                        log.warn("Finnhub API connectivity check failed");
                    }
                });
    }
    
    /**
     * Rate limiting check
     */
    private Mono<Void> checkRateLimit(String key) {
        return Mono.fromRunnable(() -> {
            long currentTime = System.currentTimeMillis();
            long windowStart = currentTime - 60000; // 1 minute window
            
            // Clean old entries
            lastRequestTimes.entrySet().removeIf(entry -> entry.getValue() < windowStart);
            requestCounts.entrySet().removeIf(entry -> !lastRequestTimes.containsKey(entry.getKey()));
            
            // Check current rate
            int currentRequests = requestCounts.getOrDefault(key, 0);
            if (currentRequests >= rateLimitPerMinute) {
                long waitTime = lastRequestTimes.get(key) + 60000 - currentTime;
                if (waitTime > 0) {
                    try {
                        Thread.sleep(Math.min(waitTime, 5000)); // Max 5 second wait
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            // Update counters
            lastRequestTimes.put(key, currentTime);
            requestCounts.put(key, currentRequests + 1);
        });
    }
    
    /**
     * Check if exception is retryable
     */
    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException webEx) {
            HttpStatus status = (HttpStatus) webEx.getStatusCode();
            return status.is5xxServerError() || 
                   status == HttpStatus.TOO_MANY_REQUESTS ||
                   status == HttpStatus.REQUEST_TIMEOUT;
        }
        return throwable instanceof java.net.ConnectException ||
               throwable instanceof java.util.concurrent.TimeoutException;
    }
    
    /**
     * Map Finnhub quote response to internal MarketData model
     */
    private MarketData mapToMarketData(String symbol, FinnhubApiResponse.Quote quote) {
        if (quote == null || !quote.isValid()) {
            return createErrorMarketData(symbol, "Invalid quote data received");
        }
        
        LocalDateTime updateTime = quote.getTimestamp() != null ? 
                LocalDateTime.ofInstant(Instant.ofEpochSecond(quote.getTimestamp()), ZoneId.systemDefault()) :
                LocalDateTime.now();
        
        return MarketData.builder()
                .symbol(symbol)
                .currentPrice(quote.getCurrentPrice())
                .dailyChange(quote.getChange())
                .dailyChangePercentage(quote.getPercentChange())
                .openPrice(quote.getOpenPrice())
                .highPrice(quote.getHighPrice())
                .lowPrice(quote.getLowPrice())
                .previousClose(quote.getPreviousClose())
                .lastUpdated(updateTime)
                .isMarketOpen(isMarketHours(updateTime))
                .dataSource("Finnhub")
                .hasError(false)
                .build();
    }
    
    /**
     * Create empty market data for missing API key scenarios
     */
    private MarketData createEmptyMarketData(String symbol) {
        return MarketData.builder()
                .symbol(symbol)
                .currentPrice(BigDecimal.ZERO)
                .dailyChange(BigDecimal.ZERO)
                .dailyChangePercentage(BigDecimal.ZERO)
                .lastUpdated(LocalDateTime.now())
                .isMarketOpen(false)
                .dataSource("None")
                .hasError(false)
                .errorMessage("API key not configured")
                .build();
    }
    
    /**
     * Create error market data for failed requests
     */
    private MarketData createErrorMarketData(String symbol, String errorMessage) {
        return MarketData.builder()
                .symbol(symbol)
                .currentPrice(BigDecimal.ZERO)
                .dailyChange(BigDecimal.ZERO)
                .dailyChangePercentage(BigDecimal.ZERO)
                .lastUpdated(LocalDateTime.now())
                .isMarketOpen(false)
                .dataSource("Finnhub")
                .hasError(true)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * Simple market hours check (US Eastern Time)
     */
    private boolean isMarketHours(LocalDateTime time) {
        // This is a simplified check - will be enhanced by MarketHoursUtil later
        int hour = time.getHour();
        int dayOfWeek = time.getDayOfWeek().getValue();
        
        // Monday to Friday, 9:30 AM to 4:00 PM ET (simplified)
        return dayOfWeek >= 1 && dayOfWeek <= 5 && hour >= 9 && hour < 16;
    }
}