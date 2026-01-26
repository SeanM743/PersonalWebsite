# Design Document: Simple Page-Load Caching with Scheduler Disable

## Overview

This design implements a simple, effective solution to eliminate excessive Finnhub API calls by:
1. **Completely disabling all scheduled/automatic API calls**
2. **Implementing simple server-side caching with 1-minute TTL**
3. **Making API calls only when users load the portfolio page**

The solution prioritizes simplicity and immediate effectiveness over complex market-aware scheduling.

## Architecture

### Simplified Architecture

```
User loads Portfolio Page
    ↓
Frontend makes API request
    ↓
Backend checks cache (1-minute TTL)
    ↓
If cache valid: Return cached data
If cache stale/missing: Call Finnhub API → Update cache → Return data
```

### Key Components

1. **Simple Cache Manager**: Basic caching with fixed 1-minute TTL
2. **Disabled Scheduler**: All automatic scheduling completely turned off
3. **Request-Only API Client**: API calls only triggered by user requests
4. **Basic Rate Limiting**: Protection against rapid successive calls

## Components and Interfaces

### Simple Cache Manager

```java
@Service
public class SimpleMarketDataCache {
    
    private final Map<String, CachedData> cache = new ConcurrentHashMap<>();
    private static final Duration CACHE_TTL = Duration.ofMinutes(1);
    
    public Optional<MarketData> getCachedData(String symbol);
    public void cacheMarketData(String symbol, MarketData data);
    public boolean isCacheValid(String symbol);
    public void clearExpiredEntries();
    public CacheStatistics getStatistics();
}
```

### Disabled Scheduler Configuration

```java
@Configuration
public class SchedulerConfiguration {
    
    @Value("${portfolio.scheduler.enabled:false}")
    private boolean schedulerEnabled; // Default to false
    
    @ConditionalOnProperty(value = "portfolio.scheduler.enabled", havingValue = "true")
    @Bean
    public MarketDataScheduler marketDataScheduler() {
        // Only create if explicitly enabled
        return new MarketDataScheduler();
    }
}
```

### Request-Only Portfolio Service

```java
@Service
public class RequestOnlyPortfolioService {
    
    private final SimpleMarketDataCache cache;
    private final FinnhubClient finnhubClient;
    private final RateLimiter rateLimiter;
    
    public PortfolioSummary getPortfolioSummary(Long userId) {
        // 1. Get user symbols
        // 2. Check cache for each symbol
        // 3. If cache miss/stale, call API (with rate limiting)
        // 4. Update cache and return data
    }
}
```

## Data Models

### Simple Cached Data

```java
public class CachedData {
    private final MarketData data;
    private final LocalDateTime cacheTime;
    private static final Duration TTL = Duration.ofMinutes(1);
    
    public boolean isValid() {
        return cacheTime.plus(TTL).isAfter(LocalDateTime.now());
    }
    
    public boolean isExpired() {
        return !isValid();
    }
    
    public MarketData getData() {
        return data;
    }
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: No Automatic API Calls
*For any* time period when no user requests are made, the system should make zero API calls to Finnhub
**Validates: Requirements 3.1, 3.2, 3.3**

### Property 2: Cache TTL Compliance
*For any* cached data, if the data is less than 1 minute old, it should be served without making an API call
**Validates: Requirements 2.2, 2.3**

### Property 3: API Call Only on Cache Miss
*For any* portfolio request, an API call should only be made if cached data doesn't exist or is older than 1 minute
**Validates: Requirements 1.2, 1.3, 1.4**

### Property 4: Rate Limiting Protection
*For any* sequence of rapid requests, actual API calls should be limited to at most one per minute
**Validates: Requirements 4.1, 4.2**

## Error Handling

### API Rate Limiting
- **Circuit Breaker**: Automatically stop API calls when rate limits are hit
- **Exponential Backoff**: Increase wait times between retries
- **Graceful Degradation**: Serve stale cached data when API is unavailable

### Cache Failures
- **Cache Miss Handling**: Fallback to API calls with rate limiting
- **Cache Corruption**: Automatic cache invalidation and rebuild
- **Memory Pressure**: LRU eviction with priority for frequently accessed symbols

### Network Issues
- **Timeout Handling**: Configurable timeouts with fallback to cached data
- **Connection Failures**: Retry logic with exponential backoff
- **Partial Failures**: Handle partial batch failures gracefully

## Testing Strategy

### Unit Tests
- Cache TTL logic validation
- Rate limiter behavior verification
- Market hours calculation accuracy
- User activity tracking correctness

### Property-Based Tests
- Cache consistency across different market conditions
- Rate limit compliance under various load patterns
- TTL behavior with different time scenarios
- Client-server cache synchronization

### Integration Tests
- End-to-end API call flow with caching
- Market hours transitions
- User activity scenarios
- Rate limiting integration

### Performance Tests
- Cache hit rate optimization
- Memory usage under load
- API call reduction verification
- Response time improvements

## Implementation Notes

### Configuration Properties
```properties
# Disable scheduler completely
portfolio.scheduler.enabled=false

# Cache settings
portfolio.cache.ttl.minutes=1
portfolio.cache.cleanup.interval.minutes=5

# Basic rate limiting
portfolio.api.min-interval-seconds=60
portfolio.api.max-concurrent-requests=1

# Monitoring
portfolio.monitoring.enabled=true
portfolio.monitoring.log-cache-stats=true
```

### Implementation Strategy
- Disable MarketDataScheduler by setting `portfolio.scheduler.enabled=false`
- Implement simple in-memory cache with 1-minute TTL
- Modify PortfolioService to check cache before API calls
- Add basic rate limiting to prevent rapid successive calls
- Remove all scheduled tasks and background updates