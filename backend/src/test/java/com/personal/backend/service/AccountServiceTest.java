package com.personal.backend.service;

import com.personal.backend.dto.PortfolioResponse;
import com.personal.backend.model.Account;
import com.personal.backend.model.StockTicker;
import com.personal.backend.repository.AccountBalanceHistoryRepository;
import com.personal.backend.repository.AccountRepository;
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

    @InjectMocks
    private AccountService accountService;

    private Account stockPortfolioAccount;
    private Account cashAccount;
    private List<StockTicker> mockHoldings;

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

        // Create mock stock holdings
        StockTicker amzn = new StockTicker();
        amzn.setSymbol("AMZN");
        amzn.setQuantity(new BigDecimal("1000"));
        amzn.setCurrentPrice(new BigDecimal("200.50"));
        // getCurrentValue() should return 1000 * 200.50 = 200,500

        StockTicker googl = new StockTicker();
        googl.setSymbol("GOOGL");
        googl.setQuantity(new BigDecimal("500"));
        googl.setCurrentPrice(new BigDecimal("150.00"));
        // getCurrentValue() should return 500 * 150.00 = 75,000

        StockTicker aapl = new StockTicker();
        aapl.setSymbol("AAPL");
        aapl.setQuantity(new BigDecimal("2000"));
        aapl.setCurrentPrice(new BigDecimal("175.25"));
        // getCurrentValue() should return 2000 * 175.25 = 350,500

        mockHoldings = Arrays.asList(amzn, googl, aapl);
        // Total portfolio value should be: 200,500 + 75,000 + 350,500 = 626,000
    }

    @Test
    void testGetAllAccounts_CalculatesStockPortfolioValueCorrectly() {
        // Arrange
        when(accountRepository.findAll()).thenReturn(Arrays.asList(stockPortfolioAccount, cashAccount));
        when(stockHoldingService.getUserStockHoldings(1L))
                .thenReturn(PortfolioResponse.success(mockHoldings, "Success"));

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

        // Verify that stockHoldingService was called
        verify(stockHoldingService, times(1)).getUserStockHoldings(1L);
    }

    @Test
    void testGetAllAccounts_HandlesNullCurrentValue() {
        // Arrange
        StockTicker tickerWithNullPrice = new StockTicker();
        tickerWithNullPrice.setSymbol("TEST");
        tickerWithNullPrice.setQuantity(new BigDecimal("100"));
        tickerWithNullPrice.setCurrentPrice(null); // getCurrentValue() will return null

        when(accountRepository.findAll()).thenReturn(Arrays.asList(stockPortfolioAccount));
        when(stockHoldingService.getUserStockHoldings(1L))
                .thenReturn(PortfolioResponse.success(Arrays.asList(mockHoldings.get(0), tickerWithNullPrice), "Success"));

        // Act
        List<Account> accounts = accountService.getAllAccounts();

        // Assert
        Account stockAccount = accounts.get(0);
        
        // Should only sum the valid holding (AMZN = 200,500), ignoring the null value
        BigDecimal expectedValue = new BigDecimal("200500.00");
        assertEquals(0, expectedValue.compareTo(stockAccount.getBalance()),
                "Stock portfolio should ignore holdings with null currentValue");
    }

    @Test
    void testGetAccount_CalculatesStockPortfolioValueCorrectly() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(stockPortfolioAccount));
        when(stockHoldingService.getUserStockHoldings(1L))
                .thenReturn(PortfolioResponse.success(mockHoldings, "Success"));

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
