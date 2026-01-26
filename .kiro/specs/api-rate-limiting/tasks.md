# Implementation Plan: Simple Page-Load Caching with Scheduler Disable

## Overview

This implementation plan provides immediate relief from excessive Finnhub API calls by completely disabling automatic scheduling and implementing simple 1-minute server-side caching. API calls will only occur when users load the portfolio page.

## Tasks

- [x] 1. Disable All Automatic Scheduling
  - Set `portfolio.scheduler.enabled=false` in configuration
  - Disable MarketDataScheduler and all scheduled tasks
  - Remove or comment out @Scheduled annotations
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 2. Implement Simple Cache Manager
  - [ ] 2.1 Create SimpleMarketDataCache class
    - Implement in-memory cache with 1-minute TTL
    - Add cache validation and expiration logic
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [ ] 2.2 Add cache cleanup and monitoring
    - Implement periodic cleanup of expired entries
    - Add basic cache statistics (hit rate, size)
    - _Requirements: 2.5, 5.3, 5.4_

  - [ ]* 2.3 Write property tests for cache manager
    - **Property 2: Cache TTL Compliance**
    - **Validates: Requirements 2.2, 2.3**

- [ ] 3. Modify Portfolio Service for Cache-First Approach
  - [ ] 3.1 Update PortfolioService to check cache before API calls
    - Modify getCompletePortfolio to use cache-first strategy
    - Only call Finnhub API if cache is empty or expired
    - _Requirements: 1.2, 1.3, 1.4_

  - [ ] 3.2 Add basic rate limiting protection
    - Implement minimum 1-minute cooldown between API calls
    - Add request queuing for rapid successive requests
    - _Requirements: 4.1, 4.2, 4.3_

  - [ ]* 3.3 Write property tests for request-only behavior
    - **Property 3: API Call Only on Cache Miss**
    - **Validates: Requirements 1.2, 1.3, 1.4**

- [ ] 4. Update Configuration
  - [ ] 4.1 Add cache configuration properties
    - Set default cache TTL to 1 minute
    - Configure cache cleanup intervals
    - _Requirements: 5.1, 5.2_

  - [ ] 4.2 Disable scheduler in application properties
    - Set `portfolio.scheduler.enabled=false`
    - Remove or disable all scheduled task configurations
    - _Requirements: 3.1, 3.2_

- [ ] 5. Add Monitoring and Logging
  - [ ] 5.1 Implement cache statistics tracking
    - Track cache hits, misses, and API calls
    - Log cache performance metrics
    - _Requirements: 5.3, 5.4, 5.5_

  - [ ] 5.2 Add API call logging
    - Log when API calls are made and why
    - Monitor for unexpected API usage patterns
    - _Requirements: 4.5, 5.4_

  - [ ]* 5.3 Write tests for monitoring functionality
    - Test cache statistics accuracy
    - Verify logging captures API calls correctly
    - _Requirements: 5.3, 5.4, 5.5_

- [ ] 6. Integration and Testing
  - [ ] 6.1 Test complete flow with disabled scheduler
    - Verify no automatic API calls are made
    - Test that page loads trigger API calls only when cache is stale
    - _Requirements: 3.1, 3.2, 3.3_

  - [ ] 6.2 Test cache behavior with multiple page loads
    - Verify first load calls API and caches data
    - Verify subsequent loads within 1 minute use cache
    - Verify loads after 1 minute call API again
    - _Requirements: 1.2, 1.3, 1.4, 1.5_

  - [ ]* 6.3 Write comprehensive integration tests
    - **Property 1: No Automatic API Calls**
    - **Validates: Requirements 3.1, 3.2, 3.3**

- [ ] 7. Performance Validation
  - [ ] 7.1 Monitor API call reduction
    - Compare API calls before and after implementation
    - Verify API calls only occur on page loads
    - _Requirements: 3.1, 3.2, 3.3_

  - [ ] 7.2 Test cache effectiveness
    - Verify high cache hit rates for repeated page loads
    - Test cache behavior during typical usage patterns
    - _Requirements: 2.2, 2.3, 5.5_

  - [ ]* 7.3 Write property tests for rate limiting
    - **Property 4: Rate Limiting Protection**
    - **Validates: Requirements 4.1, 4.2**

- [ ] 8. Cleanup and Documentation
  - [ ] 8.1 Remove unused scheduler code
    - Clean up MarketDataScheduler if no longer needed
    - Remove unused scheduled task methods
    - _Requirements: 3.1, 3.2_

  - [ ] 8.2 Update configuration documentation
    - Document new cache settings
    - Document scheduler disable configuration
    - _Requirements: 5.1, 5.2_

- [ ] 9. Final Validation
  - [ ] 9.1 Verify zero automatic API calls
    - Monitor system for 24 hours with no user activity
    - Confirm zero API calls are made during idle periods
    - _Requirements: 3.1, 3.2, 3.3_

  - [ ] 9.2 Test user experience
    - Verify portfolio page loads quickly with cached data
    - Test that data is reasonably fresh (within 1 minute)
    - _Requirements: 1.2, 1.3, 1.4, 1.5_

- [ ] 10. Checkpoint - Ensure all tests pass and no automatic API calls occur
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster implementation
- Priority should be given to tasks 1-3 for immediate relief
- This approach eliminates 90%+ of API calls by removing all automatic scheduling
- Cache TTL of 1 minute provides good balance between freshness and API usage
- Each task references specific requirements for traceability