# Requirements Document

## Introduction

The current portfolio application is making excessive calls to the Finnhub API, causing rate limiting issues. The system currently calls the API every 1-15 minutes depending on market hours, which is unnecessary for a personal portfolio dashboard. We need to implement intelligent caching and reduce API frequency to minimize costs and avoid rate limits.

## Glossary

- **Finnhub_API**: Third-party stock market data API service
- **Market_Hours**: US stock market trading hours (9:30 AM - 4:00 PM ET, Monday-Friday)
- **Cache_Manager**: System component responsible for storing and retrieving cached market data
- **Portfolio_Dashboard**: Frontend application displaying portfolio information
- **Rate_Limiting**: API provider's restriction on number of requests per time period

## Requirements

### Requirement 1: Page-Load-Only API Calls

**User Story:** As a portfolio owner, I want API calls to happen only when I load the portfolio page, so that I avoid unnecessary background API usage and rate limiting.

#### Acceptance Criteria

1. WHEN no users are accessing the portfolio page, THE System SHALL NOT make any automatic or scheduled API calls
2. WHEN a user loads the portfolio page, THE System SHALL check if cached data exists and is less than 1 minute old
3. WHEN cached data is less than 1 minute old, THE System SHALL serve cached data without calling the API
4. WHEN cached data is older than 1 minute or doesn't exist, THE System SHALL call the Finnhub API to fetch fresh data
5. WHEN a user refreshes the portfolio page multiple times within 1 minute, THE System SHALL serve cached data for subsequent requests

### Requirement 2: Simple Server-Side Caching

**User Story:** As a system administrator, I want simple server-side caching with a 1-minute TTL, so that page refreshes within 1 minute don't trigger duplicate API calls.

#### Acceptance Criteria

1. WHEN market data is fetched from Finnhub API, THE Cache_Manager SHALL store the data with a timestamp
2. WHEN a portfolio page request is made, THE Cache_Manager SHALL check if cached data exists and is less than 1 minute old
3. WHEN cached data is less than 1 minute old, THE System SHALL return cached data without calling the API
4. WHEN cached data is older than 1 minute or doesn't exist, THE System SHALL fetch fresh data and update the cache
5. WHEN the system starts up, THE Cache_Manager SHALL have an empty cache and fetch data on first request

### Requirement 3: Disable All Scheduled Updates

**User Story:** As a cost-conscious system owner, I want to completely disable all automatic and scheduled API calls, so that API usage is strictly controlled by user actions.

#### Acceptance Criteria

1. WHEN the system starts up, THE MarketDataScheduler SHALL be disabled and not make any scheduled calls
2. WHEN the system is running, THE System SHALL NOT make any background or automatic API calls
3. WHEN no users access the portfolio page for any period of time, THE System SHALL NOT make any API calls
4. WHEN the market opens or closes, THE System SHALL NOT automatically trigger API calls
5. WHEN the system has been idle for hours or days, THE System SHALL NOT make any maintenance or warmup API calls

### Requirement 4: Rate Limiting Protection

**User Story:** As a system administrator, I want basic rate limiting protection to prevent accidental API abuse, so that even if something goes wrong, we don't exceed reasonable API usage.

#### Acceptance Criteria

1. WHEN multiple API requests are made in quick succession, THE System SHALL enforce a minimum 1-minute cooldown between actual API calls
2. WHEN an API call is in progress, THE System SHALL queue additional requests and serve them from cache when the call completes
3. WHEN API rate limiting errors occur, THE System SHALL log the event and serve cached data if available
4. WHEN the API is temporarily unavailable, THE System SHALL serve cached data even if it's older than 1 minute
5. WHEN the system detects unusual API usage patterns, THE System SHALL log warnings for monitoring

### Requirement 5: Configuration and Monitoring

**User Story:** As a system administrator, I want configurable cache settings and monitoring of API usage, so that I can track the effectiveness of the caching solution.

#### Acceptance Criteria

1. WHEN configuring the system, THE Administrator SHALL be able to set the cache TTL (default 1 minute)
2. WHEN configuring the system, THE Administrator SHALL be able to enable/disable the scheduler completely
3. WHEN monitoring the system, THE Administrator SHALL see metrics on API calls made and cache hit rates
4. WHEN the system is operating normally, THE Monitoring SHALL show that API calls only occur on page loads
5. WHEN cache is working effectively, THE Monitoring SHALL show high cache hit rates for repeated page loads within 1 minute