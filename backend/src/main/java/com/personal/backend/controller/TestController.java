package com.personal.backend.controller;

import com.personal.backend.service.AccountSnapshotService;
import com.personal.backend.service.YahooFinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {
    
    private final YahooFinanceService yahooFinanceService;
    private final AccountSnapshotService accountSnapshotService;
    
    @PostMapping("/fetch-prices")
    public Map<String, Object> fetchPrices() {
        log.info("Manual trigger: fetching stock prices");
        try {
            yahooFinanceService.fetchAndPersistHistoricalPrices("AMZN", 
                LocalDate.of(2026, 1, 1), LocalDate.now());
            yahooFinanceService.fetchAndPersistHistoricalPrices("ANET", 
                LocalDate.of(2026, 1, 1), LocalDate.now());
            yahooFinanceService.fetchAndPersistHistoricalPrices("NVDA", 
                LocalDate.of(2026, 1, 1), LocalDate.now());
            yahooFinanceService.fetchAndPersistHistoricalPrices("TMUS", 
                LocalDate.of(2026, 1, 1), LocalDate.now());
            
            return Map.of("success", true, "message", "Prices fetched");
        } catch (Exception e) {
            log.error("Failed: {}", e.getMessage(), e);
            return Map.of("success", false, "message", e.getMessage());
        }
    }
    
    @PostMapping("/create-snapshots")
    public Map<String, Object> createSnapshots() {
        log.info("Manual trigger: creating snapshots");
        try {
            accountSnapshotService.createSnapshotsForDate(LocalDate.now());
            return Map.of("success", true, "message", "Snapshots created");
        } catch (Exception e) {
            log.error("Failed: {}", e.getMessage(), e);
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}
