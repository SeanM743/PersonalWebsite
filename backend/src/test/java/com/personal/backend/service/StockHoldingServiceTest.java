package com.personal.backend.service;

import com.personal.backend.dto.PortfolioResponse;
import com.personal.backend.model.StockTicker;
import com.personal.backend.model.StockTransaction;
import com.personal.backend.repository.AccountBalanceHistoryRepository;
import com.personal.backend.repository.AccountRepository;
import com.personal.backend.repository.StockTickerRepository;
import com.personal.backend.repository.StockTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockHoldingServiceTest {

    @Mock
    private StockTickerRepository stockRepository;

    @Mock
    private StockTransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountBalanceHistoryRepository historyRepository;

    @Mock
    private FinancialValidator financialValidator;

    @Mock
    private YahooFinanceService yahooFinanceService;

    @Mock
    private StockPriceCacheService stockPriceCacheService;

    private StockHoldingService stockHoldingService;

    @BeforeEach
    void setUp() {
        // We can pass null or mocks for dependencies not used in recalculateHoldings
        // Note: Check constructor arguments
        stockHoldingService = new StockHoldingService(
            stockRepository,
            transactionRepository,
            accountRepository,
            historyRepository,
            financialValidator,
            yahooFinanceService,
            stockPriceCacheService
        );
    }

    @Test
    void recalculateHoldings_SimpleBuy_CreatesNewHolding() {
        // Arrange
        Long userId = 1L;
        StockTransaction buyTxn = StockTransaction.builder()
                .userId(userId)
                .symbol("AAPL")
                .type(StockTransaction.TransactionType.BUY)
                .quantity(new BigDecimal("10"))
                .pricePerShare(new BigDecimal("150.00"))
                .totalCost(new BigDecimal("1500.00"))
                .transactionDate(LocalDate.now())
                .build();

        // recalculateHoldings fetches all transactions
        when(transactionRepository.findByUserIdOrderByTransactionDateDesc(userId))
                .thenReturn(new ArrayList<>(Collections.singletonList(buyTxn)));

        // No existing holding
        when(stockRepository.findByUserIdAndSymbol(userId, "AAPL")).thenReturn(Optional.empty());

        // Act
        PortfolioResponse<Integer> response = stockHoldingService.recalculateHoldings(userId);

        // Assert
        assertTrue(response.isSuccess());
        verify(stockRepository).save(any(StockTicker.class));

        ArgumentCaptor<StockTicker> captor = ArgumentCaptor.forClass(StockTicker.class);
        verify(stockRepository).save(captor.capture());
        StockTicker saved = captor.getValue();

        assertEquals0(new BigDecimal("10"), saved.getQuantity());
        assertEquals0(new BigDecimal("150.00"), saved.getPurchasePrice());
    }

    @Test
    void recalculateHoldings_BuyAndPartialSell_UpdatesHolding() {
        // Arrange
        Long userId = 1L;
        StockTransaction buyTxn = StockTransaction.builder()
                .userId(userId)
                .symbol("AAPL")
                .type(StockTransaction.TransactionType.BUY)
                .quantity(new BigDecimal("10"))
                .pricePerShare(new BigDecimal("100.00"))
                .totalCost(new BigDecimal("1000.00"))
                .transactionDate(LocalDate.now().minusDays(2))
                .build();

        StockTransaction sellTxn = StockTransaction.builder()
                .userId(userId)
                .symbol("AAPL")
                .type(StockTransaction.TransactionType.SELL)
                .quantity(new BigDecimal("5"))
                .pricePerShare(new BigDecimal("120.00")) // Price doesn't affect cost basis
                .totalCost(new BigDecimal("600.00"))
                .transactionDate(LocalDate.now().minusDays(1))
                .build();

        // Note: findAll logic often returns unordered list, service sorts it
        List<StockTransaction> txns = new ArrayList<>(Arrays.asList(buyTxn, sellTxn));
        when(transactionRepository.findByUserIdOrderByTransactionDateDesc(userId))
                .thenReturn(txns);

        // Simulate existing holding (state doesn't matter much as it gets updated)
        StockTicker existing = StockTicker.builder()
                .userId(userId)
                .symbol("AAPL")
                .quantity(new BigDecimal("10"))
                .purchasePrice(new BigDecimal("100.00"))
                .build();
        when(stockRepository.findByUserIdAndSymbol(userId, "AAPL")).thenReturn(Optional.of(existing));

        // Act
        PortfolioResponse<Integer> response = stockHoldingService.recalculateHoldings(userId);

        // Assert
        assertTrue(response.isSuccess());
        
        // Should update the existing ticker
        verify(stockRepository).save(existing);
        
        // Remaining Qty = 5
        // Cost Basis Per Share = 100.00 (Average cost doesn't change on sell)
        assertEquals0(new BigDecimal("5"), existing.getQuantity());
        assertEquals0(new BigDecimal("100.00"), existing.getPurchasePrice());
    }

    @Test
    void recalculateHoldings_BuyMultipleTranches_CalculatesWeightedAverage() {
        // Arrange
        Long userId = 1L;
        // Buy 1: 10 @ 100 = 1000
        StockTransaction buy1 = StockTransaction.builder()
                .userId(userId)
                .symbol("AAPL")
                .type(StockTransaction.TransactionType.BUY)
                .quantity(new BigDecimal("10"))
                .pricePerShare(new BigDecimal("100.00"))
                .totalCost(new BigDecimal("1000.00"))
                .transactionDate(LocalDate.now().minusDays(2))
                .build();

        // Buy 2: 10 @ 120 = 1200
        StockTransaction buy2 = StockTransaction.builder()
                .userId(userId)
                .symbol("AAPL")
                .type(StockTransaction.TransactionType.BUY)
                .quantity(new BigDecimal("10"))
                .pricePerShare(new BigDecimal("120.00"))
                .totalCost(new BigDecimal("1200.00"))
                .transactionDate(LocalDate.now().minusDays(1))
                .build();

        // Total Cost 2200 / 20 shares = 110.00 avg
        when(transactionRepository.findByUserIdOrderByTransactionDateDesc(userId))
                .thenReturn(new ArrayList<>(Arrays.asList(buy1, buy2)));
        
        when(stockRepository.findByUserIdAndSymbol(userId, "AAPL")).thenReturn(Optional.empty());

        // Act
        stockHoldingService.recalculateHoldings(userId);

        // Assert
        ArgumentCaptor<StockTicker> captor = ArgumentCaptor.forClass(StockTicker.class);
        verify(stockRepository).save(captor.capture());
        StockTicker saved = captor.getValue();

        assertEquals0(new BigDecimal("20"), saved.getQuantity());
        assertEquals0(new BigDecimal("110.00"), saved.getPurchasePrice());
    }

    @Test
    void recalculateHoldings_FullSell_DeletesHolding() {
         // Arrange
        Long userId = 1L;
        StockTransaction buyTxn = StockTransaction.builder()
                .userId(userId)
                .symbol("AAPL")
                .type(StockTransaction.TransactionType.BUY)
                .quantity(new BigDecimal("10"))
                .pricePerShare(new BigDecimal("100.00"))
                .totalCost(new BigDecimal("1000.00"))
                .transactionDate(LocalDate.now().minusDays(2))
                .build();

        StockTransaction sellTxn = StockTransaction.builder()
                .userId(userId)
                .symbol("AAPL")
                .type(StockTransaction.TransactionType.SELL)
                .quantity(new BigDecimal("10"))
                .pricePerShare(new BigDecimal("120.00"))
                .totalCost(new BigDecimal("1200.00"))
                .transactionDate(LocalDate.now().minusDays(1))
                .build();

        when(transactionRepository.findByUserIdOrderByTransactionDateDesc(userId))
                .thenReturn(new ArrayList<>(Arrays.asList(buyTxn, sellTxn)));

        // Existing holding to be deleted
        StockTicker existing = StockTicker.builder()
                .userId(userId)
                .symbol("AAPL")
                .quantity(new BigDecimal("10"))
                .build();
        when(stockRepository.findByUserIdAndSymbol(userId, "AAPL")).thenReturn(Optional.of(existing));

        // Act
        stockHoldingService.recalculateHoldings(userId);

        // Assert
        verify(stockRepository).delete(existing);
    }
    
    // Helper for BigDecimal comparison
    private void assertEquals0(BigDecimal expected, BigDecimal actual) {
        if (expected.compareTo(actual) != 0) {
            assertEquals(expected, actual);
        }
    }
}
