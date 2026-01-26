package com.personal.backend.service;

import com.personal.backend.model.StockTicker;
import com.personal.backend.repository.StockTickerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service to initialize portfolio data with real holdings
 * Runs automatically on application startup
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioDataInitializer implements CommandLineRunner {
    
    private final StockTickerRepository stockRepository;
    
    @Override
    @Transactional
    public void run(String... args) {
        try {
            initializeRealPortfolioData();
        } catch (Exception e) {
            log.error("Failed to initialize portfolio data: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Initialize portfolio with real stock holdings
     */
    private void initializeRealPortfolioData() {
        Long userId = 1L; // Admin user
        
        // Check if portfolio data already exists
        long existingCount = stockRepository.countByUserId(userId);
        if (existingCount > 0) {
            log.info("Portfolio data already exists for user {} ({} positions). Skipping initialization.", 
                    userId, existingCount);
            return;
        }
        
        log.info("Initializing real portfolio data for user {}", userId);
        
        // Define real portfolio holdings
        List<StockHolding> realHoldings = List.of(
            // Major Holdings (sorted by market value)
            new StockHolding("AMZN", new BigDecimal("232.38"), new BigDecimal("3462"), 
                "Amazon.com Inc - E-commerce and cloud computing giant"),
            new StockHolding("SOFI", new BigDecimal("27.48"), new BigDecimal("3500"), 
                "SoFi Technologies Inc - Digital financial services platform"),
            new StockHolding("ANET", new BigDecimal("130.77"), new BigDecimal("600"), 
                "Arista Networks Inc - Cloud networking solutions"),
            new StockHolding("QQQ", new BigDecimal("623.93"), new BigDecimal("109.903"), 
                "Invesco QQQ Trust ETF - Nasdaq 100 tracking ETF"),
            new StockHolding("CRWV", new BigDecimal("78.87"), new BigDecimal("634.128"), 
                "CoreWeave Inc Class A - AI cloud infrastructure"),
            new StockHolding("ACHR", new BigDecimal("8.13"), new BigDecimal("5000"), 
                "Archer Aviation Inc - Electric vertical takeoff aircraft"),
            new StockHolding("INTC", new BigDecimal("36.16"), new BigDecimal("906.672"), 
                "Intel Corporation - Semiconductor manufacturer"),
            new StockHolding("SOUN", new BigDecimal("10.90"), new BigDecimal("2500"), 
                "SoundHound AI Inc Class A - Voice AI technology")
        );
        
        // Create and save stock ticker entities
        LocalDateTime now = LocalDateTime.now();
        int savedCount = 0;
        BigDecimal totalInvestment = BigDecimal.ZERO;
        
        for (StockHolding holding : realHoldings) {
            try {
                StockTicker stockTicker = StockTicker.builder()
                        .symbol(holding.symbol())
                        .purchasePrice(holding.purchasePrice())
                        .quantity(holding.quantity())
                        .userId(userId)
                        .notes(holding.notes())
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                
                stockRepository.save(stockTicker);
                savedCount++;
                totalInvestment = totalInvestment.add(stockTicker.getTotalInvestment());
                
                log.debug("Added stock position: {} - {} shares at ${}", 
                        holding.symbol(), holding.quantity(), holding.purchasePrice());
                
            } catch (Exception e) {
                log.error("Failed to save stock position {}: {}", holding.symbol(), e.getMessage());
            }
        }
        
        log.info("Successfully initialized {} stock positions with total investment of ${}", 
                savedCount, totalInvestment);
        
        // Log portfolio summary
        logPortfolioSummary(userId);
    }
    
    /**
     * Log portfolio summary for verification
     */
    private void logPortfolioSummary(Long userId) {
        try {
            List<StockTicker> holdings = stockRepository.findByUserIdOrderBySymbolAsc(userId);
            
            log.info("=== Portfolio Summary for User {} ===", userId);
            log.info("Total Positions: {}", holdings.size());
            
            BigDecimal totalInvestment = BigDecimal.ZERO;
            for (StockTicker holding : holdings) {
                BigDecimal investment = holding.getTotalInvestment();
                totalInvestment = totalInvestment.add(investment);
                
                log.info("{}: {} shares @ ${} = ${}", 
                        holding.getSymbol(), 
                        holding.getQuantity(), 
                        holding.getPurchasePrice(),
                        investment);
            }
            
            log.info("Total Stock Investment: ${}", totalInvestment);
            log.info("Note: Cash positions and 401k/mutual funds are not included in stock tracking");
            log.info("=== End Portfolio Summary ===");
            
        } catch (Exception e) {
            log.error("Failed to log portfolio summary: {}", e.getMessage());
        }
    }
    
    /**
     * Record class for holding stock data
     */
    private record StockHolding(
            String symbol,
            BigDecimal purchasePrice,
            BigDecimal quantity,
            String notes
    ) {}
}