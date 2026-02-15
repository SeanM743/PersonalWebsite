package com.personal.backend.service;

import com.personal.backend.dto.PortfolioResponse;
import com.personal.backend.dto.StockRequest;
import com.personal.backend.model.Account;
import com.personal.backend.model.AccountTransaction;
import com.personal.backend.model.StockTicker;
import com.personal.backend.model.StockTransaction;
import com.personal.backend.repository.AccountRepository;
import com.personal.backend.repository.AccountTransactionRepository;
import com.personal.backend.repository.StockTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockTransactionServiceTest {

    @Mock
    private StockTransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTransactionRepository accountTransactionRepository;

    @Mock
    private StockHoldingService stockHoldingService;

    @InjectMocks
    private StockTransactionService stockTransactionService;

    private Account cashAccount;
    private Account stockAccount;

    @BeforeEach
    void setUp() {
        cashAccount = Account.builder()
                .id(1L)
                .name("Fidelity Cash")
                .balance(new BigDecimal("10000.00"))
                .type(Account.AccountType.CASH)
                .build();

        stockAccount = Account.builder()
                .id(2L)
                .name("Stock Portfolio")
                .balance(BigDecimal.ZERO)
                .type(Account.AccountType.STOCK_PORTFOLIO)
                .build();
    }

    @Test
    void addTransaction_Buy_Success() {
        // Arrange
        StockTransaction transaction = StockTransaction.builder()
                .userId(1L)
                .symbol("AAPL")
                .type(StockTransaction.TransactionType.BUY)
                .quantity(new BigDecimal("10"))
                .pricePerShare(new BigDecimal("150.00"))
                .totalCost(new BigDecimal("1500.00"))
                .transactionDate(LocalDate.now())
                .build();

        when(transactionRepository.save(any(StockTransaction.class))).thenReturn(transaction);
        when(accountRepository.findByName("Fidelity Cash")).thenReturn(cashAccount);
        when(accountRepository.findByName("Stock Portfolio")).thenReturn(stockAccount);
        
        // Mock holding service to return success (existing holding or new)
        when(stockHoldingService.getStockHolding(anyLong(), anyString()))
                .thenReturn(PortfolioResponse.error("Not found")); // Simulate new holding
        when(stockHoldingService.addStockHolding(anyLong(), any(StockRequest.class)))
                .thenReturn(PortfolioResponse.success(new StockTicker(), "Added"));

        // Act
        StockTransaction result = stockTransactionService.addTransaction(transaction);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("8500.00"), cashAccount.getBalance()); // 10000 - 1500
        verify(accountRepository).save(cashAccount);
        verify(stockHoldingService).addStockHolding(eq(1L), any(StockRequest.class));
    }

    @Test
    void addTransaction_Sell_Success() {
        // Arrange
        StockTransaction transaction = StockTransaction.builder()
                .userId(1L)
                .symbol("AAPL")
                .type(StockTransaction.TransactionType.SELL)
                .quantity(new BigDecimal("5"))
                .pricePerShare(new BigDecimal("160.00"))
                .totalCost(new BigDecimal("800.00")) // Proceeds
                .transactionDate(LocalDate.now())
                .build();

        StockTicker existingTicker = StockTicker.builder()
                .symbol("AAPL")
                .quantity(new BigDecimal("10"))
                .purchasePrice(new BigDecimal("150.00"))
                .build();

        when(transactionRepository.save(any(StockTransaction.class))).thenReturn(transaction);
        when(accountRepository.findByName("Fidelity Cash")).thenReturn(cashAccount);
        when(accountRepository.findByName("Stock Portfolio")).thenReturn(stockAccount);

        // Mock holding service to return existing holding
        when(stockHoldingService.getStockHolding(anyLong(), anyString()))
                .thenReturn(PortfolioResponse.success(existingTicker, "Found"));
        when(stockHoldingService.updateStockHolding(anyLong(), anyString(), any(StockRequest.class)))
                .thenReturn(PortfolioResponse.success(existingTicker, "Updated"));

        // Act
        StockTransaction result = stockTransactionService.addTransaction(transaction);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("10800.00"), cashAccount.getBalance()); // 10000 + 800
        verify(accountRepository).save(cashAccount);
        verify(stockHoldingService).updateStockHolding(eq(1L), eq("AAPL"), any(StockRequest.class));
    }
}
