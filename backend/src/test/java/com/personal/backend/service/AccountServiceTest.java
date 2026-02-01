package com.personal.backend.service;

import com.personal.backend.dto.PortfolioResponse;
import com.personal.backend.model.Account;
import com.personal.backend.model.StockTicker;
import com.personal.backend.repository.AccountBalanceHistoryRepository;
import com.personal.backend.repository.AccountRepository;
import com.personal.backend.repository.StockTickerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountBalanceHistoryRepository historyRepository;

    @Mock
    private StockHoldingService stockHoldingService;
    
    @Mock
    private StockTickerRepository stockTickerRepository;

    @InjectMocks
    private AccountService accountService;

    private Account stockPortfolioAccount;
    private Account cashAccount;

    @BeforeEach
    void setUp() {
        // Create mock stock portfolio account
        stockPortfolioAccount = new Account();
        stockPortfolioAccount.setId(1L);
        stockPortfolioAccount.setName("Stock Portfolio");
        stockPortfolioAccount.setType(Account.AccountType.STOCK_PORTFOLIO);
        stockPortfolioAccount.setBalance(BigDecimal.ZERO); // Should be dynamically calculated
        stockPortfolioAccount.setUpdatedAt(LocalDateTime.now());

        // Create mock cash account
        cashAccount = new Account();
        cashAccount.setId(2L);
        cashAccount.setName("Fidelity Cash");
        cashAccount.setType(Account.AccountType.CASH);
        cashAccount.setBalance(new BigDecimal("394828.35"));
        cashAccount.setUpdatedAt(LocalDateTime.now());

        // Note: mockHoldings removed - tests now mock stockTickerRepository.getTotalCurrentValueByUserId() directly
        // Expected portfolio value based on original holdings: 626,000
        // (AMZN: 1000 * 200.50 = 200,500) + (GOOGL: 500 * 150 = 75,000) + (AAPL: 2000 * 175.25 = 350,500)
    }

    @Test
    void testGetAllAccounts_CalculatesStockPortfolioValueCorrectly() {
        // Arrange
        when(accountRepository.findAll()).thenReturn(Arrays.asList(stockPortfolioAccount, cashAccount));
        // Mock the optimized repository query that replaced the StockHoldingService call
        when(stockTickerRepository.getTotalCurrentValueByUserId(1L))
                .thenReturn(new BigDecimal("626000.00"));

        // Act
        List<Account> accounts = accountService.getAllAccounts();

        // Assert
        assertNotNull(accounts);
        assertEquals(2, accounts.size());

        Account stockAccount = accounts.stream()
                .filter(a -> a.getType() == Account.AccountType.STOCK_PORTFOLIO)
                .findFirst()
                .orElse(null);

        assertNotNull(stockAccount, "Stock portfolio account should exist");
        
        // Expected total: 200,500 + 75,000 + 350,500 = 626,000
        BigDecimal expectedValue = new BigDecimal("626000.00");
        assertEquals(0, expectedValue.compareTo(stockAccount.getBalance()),
                "Stock portfolio balance should be " + expectedValue + " but was " + stockAccount.getBalance());

        // Verify that stockTickerRepository query was called (not StockHoldingService)
        verify(stockTickerRepository, times(1)).getTotalCurrentValueByUserId(1L);
    }

    @Test
    void testGetAllAccounts_HandlesNullCurrentValue() {
        // Arrange - when there are no holdings with valid prices, repository returns 0
        when(accountRepository.findAll()).thenReturn(Arrays.asList(stockPortfolioAccount));
        // The repository query handles NULL prices by excluding them (WHERE currentPrice IS NOT NULL)
        // Mock it returning only the valid holding value (AMZN = 200,500)
        when(stockTickerRepository.getTotalCurrentValueByUserId(1L))
                .thenReturn(new BigDecimal("200500.00"));

        // Act
        List<Account> accounts = accountService.getAllAccounts();

        // Assert
        Account stockAccount = accounts.get(0);
        
        // Should only sum the valid holdings, repository query excludes null prices
        BigDecimal expectedValue = new BigDecimal("200500.00");
        assertEquals(0, expectedValue.compareTo(stockAccount.getBalance()),
                "Stock portfolio should ignore holdings with null currentValue");
    }

    @Test
    void testGetAccount_CalculatesStockPortfolioValueCorrectly() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(stockPortfolioAccount));
        when(stockTickerRepository.getTotalCurrentValueByUserId(1L))
                .thenReturn(new BigDecimal("626000.00"));

        // Act
        Optional<Account> accountOpt = accountService.getAccount(1L);

        // Assert
        assertTrue(accountOpt.isPresent());
        Account account = accountOpt.get();

        BigDecimal expectedValue = new BigDecimal("626000.00");
        assertEquals(0, expectedValue.compareTo(account.getBalance()),
                "Stock portfolio balance should be calculated correctly for single account fetch");
    }

    @Test
    void testGetAccount_NonStockAccount_ReturnsStoredBalance() {
        // Arrange
        when(accountRepository.findById(2L)).thenReturn(Optional.of(cashAccount));

        // Act
        Optional<Account> accountOpt = accountService.getAccount(2L);

        // Assert
        assertTrue(accountOpt.isPresent());
        Account account = accountOpt.get();

        // Cash account balance should remain unchanged
        assertEquals(0, new BigDecimal("394828.35").compareTo(account.getBalance()));
        
        // stockHoldingService should NOT be called for non-stock accounts
        verify(stockHoldingService, never()).getUserStockHoldings(any());
    }

    @Test
    void testCreateAccount() {
        // Arrange
        Account newAccount = new Account();
        newAccount.setName("New Savings");
        newAccount.setType(Account.AccountType.CASH);
        newAccount.setBalance(new BigDecimal("10000.00"));

        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        // Act
        Account created = accountService.createAccount(newAccount);

        // Assert
        assertNotNull(created);
        assertEquals(3L, created.getId());
        assertTrue(created.getIsManual());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void testDeleteAccount_SystemAccount_ThrowsException() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(stockPortfolioAccount));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.deleteAccount(1L);
        }, "Should not allow deletion of Stock Portfolio");

        verify(accountRepository, never()).delete(any());
    }

    @Test
    void testDeleteAccount_ManualAccount_Success() {
        // Arrange
        Account manualAccount = new Account();
        manualAccount.setId(10L);
        manualAccount.setName("My Savings");
        manualAccount.setType(Account.AccountType.CASH);
        manualAccount.setIsManual(true);

        when(accountRepository.findById(10L)).thenReturn(Optional.of(manualAccount));

        // Act
        accountService.deleteAccount(10L);

        // Assert
        verify(historyRepository, times(1)).deleteAll(any());
        verify(accountRepository, times(1)).delete(manualAccount);
    }
}
