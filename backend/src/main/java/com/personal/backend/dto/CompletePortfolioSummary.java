package com.personal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Complete portfolio summary including stocks, cash, and retirement accounts
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompletePortfolioSummary {
    
    // Stock Portfolio (tracked via APIs)
    private PortfolioSummary stockPortfolio;
    
    // Static Holdings (not tracked via APIs)
    private StaticHoldings staticHoldings;
    
    // Overall Portfolio Totals
    private BigDecimal totalPortfolioValue;
    private BigDecimal totalCashValue;
    private BigDecimal totalStockValue;
    private BigDecimal totalRetirementValue;
    private BigDecimal totalOtherInvestments;
    
    // Portfolio Allocation
    private Map<String, BigDecimal> allocationByType;
    private Map<String, BigDecimal> allocationByAssetClass;
    
    // Metadata
    private LocalDateTime lastUpdated;
    private String currency;
    private List<String> notes;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StaticHoldings {
        
        // Cash Positions
        private BigDecimal cashPosition; // FCASH
        private BigDecimal fidelityCashReserves; // FDRXX
        private BigDecimal totalCash;
        
        // Retirement Accounts (TSP/401k)
        private BigDecimal sFund; // S Fund TSP
        private BigDecimal gFund; // G Fund TSP
        private BigDecimal vanguardTarget2045; // O24H
        private BigDecimal totalRetirement;
        
        // Other Portfolios
        private BigDecimal nhPortfolio2027; // NH2027959
        private BigDecimal nhCollegePortfolio; // NH0000909
        private BigDecimal totalOtherPortfolios;
        
        // Summary
        private BigDecimal totalStaticValue;
        private LocalDateTime asOfDate;
        private List<StaticHolding> holdings;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StaticHolding {
        private String name;
        private String ticker;
        private BigDecimal shares;
        private BigDecimal pricePerShare;
        private BigDecimal marketValue;
        private String assetType;
        private String exchange;
        private String notes;
    }
    
    /**
     * Create complete portfolio summary with static holdings
     */
    public static CompletePortfolioSummary createWithStaticHoldings(PortfolioSummary stockPortfolio) {
        
        // Define static holdings based on real portfolio data
        List<StaticHolding> staticHoldingsList = List.of(
            StaticHolding.builder()
                .name("Cash")
                .ticker("FCASH")
                .shares(new BigDecimal("394117.62"))
                .pricePerShare(BigDecimal.ONE)
                .marketValue(new BigDecimal("394117.62"))
                .assetType("Cash")
                .exchange("N/A")
                .notes("Primary cash position")
                .build(),
            
            StaticHolding.builder()
                .name("S Fund")
                .ticker("S Fund")
                .shares(new BigDecimal("2030.99"))
                .pricePerShare(new BigDecimal("95.03"))
                .marketValue(new BigDecimal("193020.35"))
                .assetType("Mutual Fund")
                .exchange("TSP/401k")
                .notes("TSP Small Cap Stock Index Fund")
                .build(),
            
            StaticHolding.builder()
                .name("Vanguard Target 2045")
                .ticker("O24H")
                .shares(new BigDecimal("774.82"))
                .pricePerShare(new BigDecimal("152.63"))
                .marketValue(new BigDecimal("118260.78"))
                .assetType("Mutual Fund")
                .exchange("Vanguard")
                .notes("Target date retirement fund")
                .build(),
            
            StaticHolding.builder()
                .name("NH Portfolio 2027")
                .ticker("NH2027959")
                .shares(new BigDecimal("1919.71"))
                .pricePerShare(new BigDecimal("25.01"))
                .marketValue(new BigDecimal("48011.94"))
                .assetType("Portfolio")
                .exchange("N/A")
                .notes("Managed portfolio with 2027 target")
                .build(),
            
            StaticHolding.builder()
                .name("NH College Portfolio")
                .ticker("NH0000909")
                .shares(new BigDecimal("811.478"))
                .pricePerShare(new BigDecimal("28.54"))
                .marketValue(new BigDecimal("23159.58"))
                .assetType("Portfolio")
                .exchange("N/A")
                .notes("Education savings portfolio")
                .build(),
            
            StaticHolding.builder()
                .name("G Fund")
                .ticker("G Fund")
                .shares(new BigDecimal("244.86"))
                .pricePerShare(new BigDecimal("19.22"))
                .marketValue(new BigDecimal("4707.43"))
                .assetType("Mutual Fund")
                .exchange("TSP/401k")
                .notes("TSP Government Securities Investment Fund")
                .build(),
            
            StaticHolding.builder()
                .name("Fidelity Cash Reserves")
                .ticker("FDRXX")
                .shares(new BigDecimal("3552.12"))
                .pricePerShare(BigDecimal.ONE)
                .marketValue(new BigDecimal("3552.12"))
                .assetType("Money Market")
                .exchange("Fidelity")
                .notes("Money market fund")
                .build()
        );
        
        // Calculate static holdings totals
        BigDecimal totalCash = new BigDecimal("394117.62").add(new BigDecimal("3552.12")); // FCASH + FDRXX
        BigDecimal totalRetirement = new BigDecimal("193020.35").add(new BigDecimal("118260.78")).add(new BigDecimal("4707.43")); // S Fund + O24H + G Fund
        BigDecimal totalOtherPortfolios = new BigDecimal("48011.94").add(new BigDecimal("23159.58")); // NH portfolios
        BigDecimal totalStaticValue = staticHoldingsList.stream()
                .map(StaticHolding::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        StaticHoldings staticHoldings = StaticHoldings.builder()
                .cashPosition(new BigDecimal("394117.62"))
                .fidelityCashReserves(new BigDecimal("3552.12"))
                .totalCash(totalCash)
                .sFund(new BigDecimal("193020.35"))
                .gFund(new BigDecimal("4707.43"))
                .vanguardTarget2045(new BigDecimal("118260.78"))
                .totalRetirement(totalRetirement)
                .nhPortfolio2027(new BigDecimal("48011.94"))
                .nhCollegePortfolio(new BigDecimal("23159.58"))
                .totalOtherPortfolios(totalOtherPortfolios)
                .totalStaticValue(totalStaticValue)
                .asOfDate(LocalDateTime.now())
                .holdings(staticHoldingsList)
                .build();
        
        // Calculate overall totals
        BigDecimal stockValue = stockPortfolio != null ? stockPortfolio.getTotalValue() : BigDecimal.ZERO;
        BigDecimal totalPortfolioValue = stockValue.add(totalStaticValue);
        
        // Calculate allocations
        Map<String, BigDecimal> allocationByType = Map.of(
                "Stocks", stockValue,
                "Cash", totalCash,
                "Retirement", totalRetirement,
                "Other Investments", totalOtherPortfolios
        );
        
        Map<String, BigDecimal> allocationByAssetClass = Map.of(
                "Equities", stockValue.add(new BigDecimal("193020.35")), // Stocks + S Fund
                "Fixed Income", new BigDecimal("4707.43"), // G Fund
                "Target Date Funds", new BigDecimal("118260.78"), // O24H
                "Cash & Cash Equivalents", totalCash,
                "Managed Portfolios", totalOtherPortfolios
        );
        
        return CompletePortfolioSummary.builder()
                .stockPortfolio(stockPortfolio)
                .staticHoldings(staticHoldings)
                .totalPortfolioValue(totalPortfolioValue)
                .totalCashValue(totalCash)
                .totalStockValue(stockValue)
                .totalRetirementValue(totalRetirement)
                .totalOtherInvestments(totalOtherPortfolios)
                .allocationByType(allocationByType)
                .allocationByAssetClass(allocationByAssetClass)
                .lastUpdated(LocalDateTime.now())
                .currency("USD")
                .notes(List.of(
                    "Stock positions are updated with real-time market data",
                    "Static holdings (cash, 401k, portfolios) are based on last known values",
                    "Total portfolio value: $" + totalPortfolioValue.toString()
                ))
                .build();
    }
}