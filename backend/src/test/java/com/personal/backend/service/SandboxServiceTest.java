package com.personal.backend.service;

import com.personal.backend.dto.SandboxTradeRequest;
import com.personal.backend.model.SandboxHolding;
import com.personal.backend.model.SandboxPortfolio;
import com.personal.backend.model.SandboxTransaction;
import com.personal.backend.repository.SandboxHoldingRepository;
import com.personal.backend.repository.SandboxPortfolioRepository;
import com.personal.backend.repository.SandboxTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SandboxServiceTest {

    @Mock
    private SandboxPortfolioRepository portfolioRepository;
    @Mock
    private SandboxHoldingRepository holdingRepository;
    @Mock
    private SandboxTransactionRepository transactionRepository;
    @Mock
    private YahooFinanceService yahooFinanceService;

    @InjectMocks
    private SandboxService sandboxService;

    private SandboxPortfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = SandboxPortfolio.builder()
                .id(1L)
                .userId(1L)
                .name("Test Portfolio")
                .initialBalance(new BigDecimal("10000.00"))
                .currentBalance(new BigDecimal("10000.00"))
                .build();
    }

    @Test
    void testExecuteTrade_BuySuccess() {
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));
        
        SandboxTradeRequest request = new SandboxTradeRequest();
        request.setSymbol("AAPL");
        request.setType(SandboxTransaction.TransactionType.BUY);
        request.setQuantity(new BigDecimal("10"));
        request.setPrice(new BigDecimal("150.00"));
        request.setDate(LocalDate.now());

        when(holdingRepository.findByPortfolioIdAndSymbol(1L, "AAPL")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(SandboxTransaction.class))).thenAnswer(i -> i.getArguments()[0]);

        SandboxTransaction txn = sandboxService.executeTrade(1L, request);

        assertNotNull(txn);
        assertEquals(new BigDecimal("1500.00"), txn.getTotalCost());
        
        // Portfolio balance updated
        assertEquals(new BigDecimal("8500.00"), portfolio.getCurrentBalance());
        verify(portfolioRepository).save(portfolio);
        
        // Holding updated
        ArgumentCaptor<SandboxHolding> holdingCaptor = ArgumentCaptor.forClass(SandboxHolding.class);
        verify(holdingRepository).save(holdingCaptor.capture());
        SandboxHolding savedHolding = holdingCaptor.getValue();
        assertEquals(new BigDecimal("10"), savedHolding.getQuantity());
        assertEquals(new BigDecimal("150.0000"), savedHolding.getAverageCost());
    }

    @Test
    void testExecuteTrade_BuyInsufficientFunds() {
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));
        
        SandboxTradeRequest request = new SandboxTradeRequest();
        request.setSymbol("AAPL");
        request.setType(SandboxTransaction.TransactionType.BUY);
        request.setQuantity(new BigDecimal("100")); // 100 * 150 = 15000 > 10000
        request.setPrice(new BigDecimal("150.00"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> sandboxService.executeTrade(1L, request));
        assertTrue(exception.getMessage().contains("Insufficient funds"));
    }

    @Test
    void testExecuteTrade_SellSuccess() {
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));
        
        SandboxHolding existingHolding = SandboxHolding.builder()
                .portfolio(portfolio)
                .symbol("AAPL")
                .quantity(new BigDecimal("10"))
                .averageCost(new BigDecimal("150.00"))
                .build();
                
        when(holdingRepository.findByPortfolioIdAndSymbol(1L, "AAPL")).thenReturn(Optional.of(existingHolding));
        
        SandboxTradeRequest request = new SandboxTradeRequest();
        request.setSymbol("AAPL");
        request.setType(SandboxTransaction.TransactionType.SELL);
        request.setQuantity(new BigDecimal("5"));
        request.setPrice(new BigDecimal("200.00")); // selling at profit
        
        when(transactionRepository.save(any(SandboxTransaction.class))).thenAnswer(i -> i.getArguments()[0]);

        SandboxTransaction txn = sandboxService.executeTrade(1L, request);

        assertNotNull(txn);
        assertEquals(new BigDecimal("1000.00"), txn.getTotalCost());
        
        // Portfolio balance updated
        assertEquals(new BigDecimal("11000.00"), portfolio.getCurrentBalance());
        
        // Holding updated
        ArgumentCaptor<SandboxHolding> holdingCaptor = ArgumentCaptor.forClass(SandboxHolding.class);
        verify(holdingRepository).save(holdingCaptor.capture());
        assertEquals(new BigDecimal("5"), holdingCaptor.getValue().getQuantity());
        assertEquals(new BigDecimal("150.00"), holdingCaptor.getValue().getAverageCost()); // avg cost unchanged
    }

    @Test
    void testExecuteTrade_SellInsufficientQuantity() {
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(portfolio));
        
        SandboxHolding existingHolding = SandboxHolding.builder()
                .portfolio(portfolio)
                .symbol("AAPL")
                .quantity(new BigDecimal("10"))
                .averageCost(new BigDecimal("150.00"))
                .build();
                
        when(holdingRepository.findByPortfolioIdAndSymbol(1L, "AAPL")).thenReturn(Optional.of(existingHolding));
        
        SandboxTradeRequest request = new SandboxTradeRequest();
        request.setSymbol("AAPL");
        request.setType(SandboxTransaction.TransactionType.SELL);
        request.setQuantity(new BigDecimal("15")); // trying to sell 15 when own 10
        request.setPrice(new BigDecimal("200.00"));

        assertThrows(IllegalArgumentException.class, () -> sandboxService.executeTrade(1L, request));
    }
}
