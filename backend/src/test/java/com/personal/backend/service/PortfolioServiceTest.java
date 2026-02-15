package com.personal.backend.service;

import com.personal.backend.dto.PortfolioResponse;
import com.personal.backend.dto.CompletePortfolioSummary;
import com.personal.backend.model.Account;
import com.personal.backend.model.AccountBalanceHistory;
import com.personal.backend.repository.AccountBalanceHistoryRepository;
import com.personal.backend.repository.AccountRepository;
import com.personal.backend.repository.AccountTransactionRepository;
import com.personal.backend.service.StockHoldingService;
import com.personal.backend.service.YahooFinanceService;
import com.personal.backend.dto.PortfolioSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PortfolioServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountBalanceHistoryRepository accountBalanceHistoryRepository;

    @Mock
    private StockHoldingService stockHoldingService;
    
    // ... (rest of mocks)
    
    // Updates to usage in test methods

    
    // We need to mock these to avoid NPEs during service initialization or execution
    @Mock private YahooFinanceService yahooFinanceService;
    @Mock private MarketDataCacheManager cacheManager;
    @Mock private PerformanceCalculator performanceCalculator;
    @Mock private MarketDataScheduler marketDataScheduler;
    @Mock private AccountTransactionRepository accountTransactionRepository;
    @Mock private com.personal.backend.repository.StockTransactionRepository stockTransactionRepository;
    @Mock private HistoricalPortfolioService historicalPortfolioService;
    @Mock private StockPriceCacheService stockPriceCacheService;

    @InjectMocks
    private PortfolioService portfolioService;

    @Test
    void testGetCompletePortfolioSummary_YTD_TransferScenario() {
        // Arrange
        Long userId = 1L;
        LocalDate jan1 = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate jan2 = jan1.plusDays(1);
        LocalDate today = LocalDate.now();

        // 1. Mock Current Accounts (Today)
        Account cashAccount = Account.builder().id(1L).name("Cash").type(Account.AccountType.CASH).balance(BigDecimal.ZERO).build();
        Account stockAccount = Account.builder().id(2L).name("Stock").type(Account.AccountType.STOCK_PORTFOLIO).balance(new BigDecimal("1000.00")).build();

        when(accountRepository.findAll()).thenReturn(Arrays.asList(cashAccount, stockAccount));

        // 2. Mock Stock Holdings (Current Value = $1000)
        com.personal.backend.model.StockTicker ticker = com.personal.backend.model.StockTicker.builder()
                .symbol("AAPL")
                .quantity(new BigDecimal("10"))
                .purchasePrice(new BigDecimal("100.00"))
                .currentPrice(new BigDecimal("100.00")) // Value = 1000
                .lastPriceUpdate(java.time.LocalDateTime.now())
                .build();
        
        when(stockHoldingService.getUserStockHoldings(userId))
                .thenReturn(PortfolioResponse.success(Collections.singletonList(ticker), "Success"));

        // Mock Performance Metrics (Current Value = 1000)
        com.personal.backend.service.PerformanceCalculator.PerformanceMetrics metrics = 
            new com.personal.backend.service.PerformanceCalculator.PerformanceMetrics(
                new BigDecimal("1000.00"), // totalInvestment
                new BigDecimal("1000.00"), // currentValue
                BigDecimal.ZERO, // totalGainLoss
                BigDecimal.ZERO, // totalGainLossPct
                BigDecimal.ZERO, // dailyChange
                BigDecimal.ZERO, // dailyChangePct
                1, // totalPositions
                1, // positionsWithData
                java.time.LocalDateTime.now()
            );
            
        when(performanceCalculator.calculatePerformanceMetrics(anyList(), anyMap())).thenReturn(metrics);

        // 3. Mock History for YTD Calculation
        // Jan 1: Cash = 1000, Stock = 0 (Total 1000)
        AccountBalanceHistory h1 = AccountBalanceHistory.builder().date(jan1).accountId(1L).balance(new BigDecimal("1000.00")).build();
        // Jan 2: Cash = 0, Stock = 1000 (Transfer happenend) (Total 1000)
        AccountBalanceHistory h2_cash = AccountBalanceHistory.builder().date(jan2).accountId(1L).balance(BigDecimal.ZERO).build();
        // For Stock account, maybe history starts Jan 2
        AccountBalanceHistory h2_stock = AccountBalanceHistory.builder().date(jan2).accountId(2L).balance(new BigDecimal("1000.00")).build();
        
        // Return mixed history list sorted by date
        when(accountBalanceHistoryRepository.findByDateBetweenOrderByDateAsc(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(h1, h2_cash, h2_stock));
        
        // Act
        PortfolioResponse<CompletePortfolioSummary> response = portfolioService.getCompletePortfolioSummary(userId);

        // Assert
        assertTrue(response.isSuccess());
        CompletePortfolioSummary summary = response.getData();
        
        // Verify Values
        // Current Value = Stock (1000) + Cash (0) = 1000
        assertEquals(0, new BigDecimal("1000.00").compareTo(summary.getTotalPortfolioValue()), "Current Value should be 1000");
        
        // Expected YTD Gain: Current(1000) - Baseline(Jan 1 Total: 1000) = 0
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getTotalGainLossYTD()), "YTD Gain should be 0 (Transfer scenario)");
    }

    @Test
    void testGetPortfolioSummary_StockYTD_NetInvestment() {
        // Scenario: 
        // Jan 1: Stock Value = 1000.
        // Feb 1: Buy new stock for 100.
        // Current: Stock Value = 1100.
        // Naive YTD = 1100 - 1000 = 100 (Wrong).
        // Net Investment YTD = 1100 - (1000 + 100) = 0 (Correct).

        Long userId = 1L;
        LocalDate startOfYear = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate today = LocalDate.now();

        // 1. Mock Stock Account
        Account stockAccount = Account.builder().id(2L).name("Stock").type(Account.AccountType.STOCK_PORTFOLIO).balance(new BigDecimal("1100.00")).build();
        when(accountRepository.findByType(Account.AccountType.STOCK_PORTFOLIO)).thenReturn(Collections.singletonList(stockAccount));

        // 2. Mock History (Jan 1)
        AccountBalanceHistory h1 = AccountBalanceHistory.builder().date(startOfYear).accountId(2L).balance(new BigDecimal("1000.00")).build();
        when(accountBalanceHistoryRepository.findByAccountIdAndDateBetweenOrderByDateAsc(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(h1));

        // 3. Mock Holdings (matches current value)
        com.personal.backend.model.StockTicker ticker = com.personal.backend.model.StockTicker.builder()
                .symbol("AAPL")
                .currentPrice(new BigDecimal("1100.00"))
                .lastPriceUpdate(java.time.LocalDateTime.now())
                .build();
         when(stockHoldingService.getUserStockHoldings(userId))
                .thenReturn(PortfolioResponse.success(Collections.singletonList(ticker), "Success"));

        // 4. Mock Performance Metrics
        com.personal.backend.service.PerformanceCalculator.PerformanceMetrics metrics = 
             new com.personal.backend.service.PerformanceCalculator.PerformanceMetrics(
                 new BigDecimal("1100.00"), // totalInvestment
                 new BigDecimal("1100.00"), // currentValue
                 BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1, 1, java.time.LocalDateTime.now());
        when(performanceCalculator.calculatePerformanceMetrics(anyList(), anyMap())).thenReturn(metrics);

        // 5. Mock Transactions (The Net Investment)
        com.personal.backend.model.StockTransaction buyTxn = com.personal.backend.model.StockTransaction.builder()
                .type(com.personal.backend.model.StockTransaction.TransactionType.BUY)
                .totalCost(new BigDecimal("100.00"))
                .transactionDate(startOfYear.plusMonths(1)) // Feb 1
                .build();
        
        when(stockTransactionRepository.findByUserIdAndTransactionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(buyTxn));

        // Act
        PortfolioResponse<PortfolioSummary> response = portfolioService.getPortfolioSummary(userId);

        // Assert
        assertTrue(response.isSuccess());
        PortfolioSummary summary = response.getData();

        // Check Stock YTD Gain
        // Expected: (1100 - 1000) - 100 = 0
        assertNotNull(summary.getTotalGainLossYTD());
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getTotalGainLossYTD()), "Stock YTD Gain should be 0 after adjusting for 100 buy");
        
        // Start(1000) + NetFlow(100) = 1100 adjusted baseline
        // Gain(0) / 1100 = 0%
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getTotalGainLossPercentageYTD()), "Stock YTD Pct should be 0");
    }

    @Test
    void testGetPortfolioSummary_PeriodReturns() {
        // Scenario:
        // Today = 2026-06-01 (Assuming mocked, or just relative dates)
        // Current Value: $1100
        //
        // 7 Days Ago: Value $1050. Net Flow 0. 
        // -> Gain = 1100 - 1050 = 50.
        //
        // 1 Month Ago: Value $1200. Net Flow 0.
        // -> Gain = 1100 - 1200 = -100.
        
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        Long stockAccountId = 2L;

        // 1. Mock Stock Account
        Account stockAccount = Account.builder().id(stockAccountId).name("Stock").type(Account.AccountType.STOCK_PORTFOLIO).balance(new BigDecimal("1100.00")).build();
        when(accountRepository.findByType(eq(Account.AccountType.STOCK_PORTFOLIO))).thenReturn(Collections.singletonList(stockAccount));

        // Mock Holdings (Must not be empty to proceed to performace calc)
        com.personal.backend.model.StockTicker ticker = com.personal.backend.model.StockTicker.builder()
                .symbol("AAPL")
                .currentPrice(new BigDecimal("1100.00"))
                .quantity(BigDecimal.ONE)
                .lastPriceUpdate(java.time.LocalDateTime.now())
                .build();
        when(stockHoldingService.getUserStockHoldings(userId))
            .thenReturn(PortfolioResponse.success(Collections.singletonList(ticker), "Success"));

        // 2. Mock Performance Metrics
        com.personal.backend.service.PerformanceCalculator.PerformanceMetrics metrics = 
             new com.personal.backend.service.PerformanceCalculator.PerformanceMetrics(
                 new BigDecimal("1100.00"), // totalInvestment
                 new BigDecimal("1100.00"), // currentValue
                 BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1, 1, java.time.LocalDateTime.now());
        when(performanceCalculator.calculatePerformanceMetrics(anyList(), anyMap())).thenReturn(metrics);

        // 3. Mock Histories for specific periods using thenAnswer for robustness
        LocalDate start7d = today.minusDays(7);
        LocalDate start1m = today.minusMonths(1);
        LocalDate startOfYear = LocalDate.of(today.getYear(), 1, 1);
        
        AccountBalanceHistory h7d = AccountBalanceHistory.builder().date(start7d).accountId(stockAccountId).balance(new BigDecimal("1050.00")).build();
        AccountBalanceHistory h1m = AccountBalanceHistory.builder().date(start1m).accountId(stockAccountId).balance(new BigDecimal("1200.00")).build();
        AccountBalanceHistory hYtd = AccountBalanceHistory.builder().date(startOfYear).accountId(stockAccountId).balance(new BigDecimal("1000.00")).build();
        
        when(accountBalanceHistoryRepository.findByAccountIdAndDateBetweenOrderByDateAsc(
                any(), any(), any()))
                .thenAnswer(invocation -> {
                    LocalDate start = invocation.getArgument(1);
                    
                    if (start.isEqual(start7d)) {
                        return Collections.singletonList(h7d);
                    }
                    if (start.isEqual(start1m)) {
                        return Collections.singletonList(h1m);
                    }
                    if (start.isEqual(startOfYear)) {
                        return Collections.singletonList(hYtd);
                    }
                    return Collections.emptyList();
                });

        // 4. Mock Transactions (No flows for simplicity)
        when(stockTransactionRepository.findByUserIdAndTransactionDateBetween(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Act
        PortfolioResponse<PortfolioSummary> response = portfolioService.getPortfolioSummary(userId);

        // Verify interaction
        verify(accountBalanceHistoryRepository, atLeastOnce()).findByAccountIdAndDateBetweenOrderByDateAsc(any(), any(), any());

        // Assert
        assertTrue(response.isSuccess());
        PortfolioSummary summary = response.getData();

        // Verify 7d Gain: 1100 - 1050 = 50
        assertNotNull(summary.getTotalGainLoss7d());
        assertEquals(0, new BigDecimal("50.00").compareTo(summary.getTotalGainLoss7d()), "7d Gain should be 50");
        
        // Verify 1m Gain: 1100 - 1200 = -100
        assertNotNull(summary.getTotalGainLoss1m());
        assertEquals(0, new BigDecimal("-100.00").compareTo(summary.getTotalGainLoss1m()), "1m Gain should be -100");
    }
}
