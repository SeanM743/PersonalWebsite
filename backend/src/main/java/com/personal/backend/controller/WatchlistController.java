package com.personal.backend.controller;

import com.personal.backend.dto.AccountResponse;
import com.personal.backend.dto.MarketData;
import com.personal.backend.model.CurrentStockPrice;
import com.personal.backend.model.WatchlistItem;
import com.personal.backend.repository.WatchlistRepository;
import com.personal.backend.service.StockPriceCacheService;
import com.personal.backend.service.YahooFinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
@Slf4j
public class WatchlistController {

    private final WatchlistRepository watchlistRepository;
    private final com.personal.backend.service.StockPriceCacheService stockPriceCacheService;
    private final com.personal.backend.service.YahooFinanceService yahooFinanceService;

    @GetMapping
    public ResponseEntity<AccountResponse<List<WatchlistDetail>>> getWatchlist() {
        Long userId = 1L; // Admin
        List<WatchlistItem> items = watchlistRepository.findByUserIdOrderBySymbolAsc(userId);
        
        List<String> symbols = items.stream()
                .map(WatchlistItem::getSymbol)
                .toList();

        Map<String, com.personal.backend.model.CurrentStockPrice> prices = stockPriceCacheService.getPricesWithRefresh(symbols);
        
        List<WatchlistDetail> details = items.stream().map(item -> {
            com.personal.backend.model.CurrentStockPrice price = prices.get(item.getSymbol());
            return WatchlistDetail.builder()
                    .id(item.getId())
                    .symbol(item.getSymbol())
                    .companyName(price != null && price.getCompanyName() != null ? price.getCompanyName() : "Unknown")
                    .currentPrice(price != null && price.getPrice() != null ? price.getPrice().doubleValue() : 0.0)
                    .dailyChange(price != null && price.getDailyChange() != null ? price.getDailyChange().doubleValue() : 0.0)
                    .dailyChangePercent(price != null && price.getDailyChangePercent() != null ? price.getDailyChangePercent().doubleValue() : 0.0)
                    .marketOpen(price != null ? price.getMarketOpenWhenFetched() : false)
                    .build();
        }).toList();
        
        return ResponseEntity.ok(AccountResponse.success(details, "Watchlist retrieved"));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<AccountResponse<WatchlistDetail>> addToWatchlist(@RequestBody @Valid WatchlistRequest request) {
        Long userId = 1L;
        String symbol = request.getSymbol().toUpperCase();
        
        // Check duplicate
        if (watchlistRepository.findByUserIdAndSymbol(userId, symbol).isPresent()) {
            return ResponseEntity.badRequest().body(AccountResponse.error("Symbol already in watchlist"));
        }

        // Validate symbol and get initial data
        var marketDataOpt = yahooFinanceService.getMarketData(symbol);
        if (marketDataOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(AccountResponse.error("Invalid symbol or data unavailable"));
        }
        var marketData = marketDataOpt.get();

        // Update cache immediately
        stockPriceCacheService.updatePrice(symbol, marketData);
        
        WatchlistItem item = WatchlistItem.builder()
                .userId(userId)
                .symbol(symbol)
                .build();
                
        WatchlistItem saved = watchlistRepository.save(item);
        
        WatchlistDetail detail = WatchlistDetail.builder()
                .id(saved.getId())
                .symbol(saved.getSymbol())
                .companyName(marketData.getCompanyName())
                .currentPrice(marketData.getCurrentPrice() != null ? marketData.getCurrentPrice().doubleValue() : null)
                .dailyChange(marketData.getDailyChange() != null ? marketData.getDailyChange().doubleValue() : null)
                .dailyChangePercent(marketData.getDailyChangePercentage() != null ? marketData.getDailyChangePercentage().doubleValue() : null)
                .marketOpen(stockPriceCacheService.isMarketOpen())
                .build();

        return ResponseEntity.ok(AccountResponse.success(detail, "Added to watchlist"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AccountResponse<Void>> removeFromWatchlist(@PathVariable Long id) {
        watchlistRepository.deleteById(id);
        return ResponseEntity.ok(AccountResponse.success(null, "Removed from watchlist"));
    }

    @lombok.Data
    public static class WatchlistRequest {
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Pattern(regexp = "^[A-Z]{1,10}$", message = "Stock symbol must be 1-10 uppercase letters")
        private String symbol;
    }

    @lombok.Data
    @lombok.Builder
    public static class WatchlistDetail {
        private Long id;
        private String symbol;
        private String companyName;
        private Double currentPrice;
        private Double dailyChange;
        private Double dailyChangePercent;
        private Boolean marketOpen;
    }
}
