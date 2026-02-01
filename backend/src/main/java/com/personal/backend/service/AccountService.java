package com.personal.backend.service;

import com.personal.backend.dto.AccountResponse;
import com.personal.backend.model.Account;
import com.personal.backend.model.AccountBalanceHistory;
import com.personal.backend.model.StockTicker;
import com.personal.backend.repository.AccountBalanceHistoryRepository;
import com.personal.backend.repository.AccountRepository;
import com.personal.backend.repository.StockTickerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountBalanceHistoryRepository historyRepository;
    private final StockHoldingService stockHoldingService;
    private final StockTickerRepository stockTickerRepository;

    /**
     * Get all accounts for a user (Currently single user system, so just get all)
     */
    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        List<Account> accounts = accountRepository.findAll();
        // Dynamically update stock portfolio value
        Long userId = 1L; // User Context
        BigDecimal stockValue = calculateTotalStockValue(userId);
        
        accounts.stream()
                .filter(a -> a.getType() == Account.AccountType.STOCK_PORTFOLIO)
                .forEach(a -> a.setBalance(stockValue));
                
        return accounts;
    }
    
    /**
     * Get specific account
     */
    @Transactional(readOnly = true)
    public Optional<Account> getAccount(Long id) {
        Optional<Account> accountOpt = accountRepository.findById(id);
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            if (account.getType() == Account.AccountType.STOCK_PORTFOLIO) {
                Long userId = 1L;
                account.setBalance(calculateTotalStockValue(userId));
            }
            return Optional.of(account);
        }
        return accountOpt;
    }

    /**
     * Calculate total stock portfolio value using optimized single-query approach
     * This replaces the N+1 query pattern that went through StockHoldingService
     */
    private BigDecimal calculateTotalStockValue(Long userId) {
        try {
            BigDecimal totalValue = stockTickerRepository.getTotalCurrentValueByUserId(userId);
            log.debug("Calculated total stock value for user {}: {}", userId, totalValue);
            return totalValue != null ? totalValue : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Error calculating total stock value for user {}: {}", userId, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Explicitly update account balance (e.g. user manually updates Roth IRA)
     */
    public Account updateBalance(Long accountId, BigDecimal newBalance) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setBalance(newBalance);
        account = accountRepository.save(account);
        
        // Record history snapshot for "today"
        recordDailySnapshot(account);
        
        return account;
    }
    
    /**
     * Update account details (name, type, balance, notes)
     */
    public Account updateAccount(Long id, Account details) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
                
        // Protect system accounts from critical changes
        if (account.getType() == Account.AccountType.STOCK_PORTFOLIO) {
            // Only allow updating notes for stock portfolio
            account.setNotes(details.getNotes());
        } else {
            account.setName(details.getName());
            account.setType(details.getType());
            account.setBalance(details.getBalance());
            account.setNotes(details.getNotes());
            
            // Record snapshot if balance changed
            recordDailySnapshot(account);
        }
        
        return accountRepository.save(account);
    }
    
    /**
     * Create a new manual account
     */
    public Account createAccount(Account account) {
        account.setIsManual(true);
        // Ensure type is set, default to OTHER if null
        if (account.getType() == null) {
            account.setType(Account.AccountType.OTHER);
        }
        return accountRepository.save(account);
    }

    /**
     * Delete an account
     */
    public void deleteAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Protect system accounts
        if (account.getType() == Account.AccountType.STOCK_PORTFOLIO || 
            "Fidelity Cash".equals(account.getName())) {
            throw new IllegalArgumentException("Cannot delete system account: " + account.getName());
        }

        // Delete associated history first (cascading usually handles this but being safe)
        historyRepository.deleteAll(historyRepository.findByAccountId(accountId));
        
        accountRepository.delete(account);
    }

    /**
     * Internal: Record a history snapshot. If one exists for today, update it.
     */
    private void recordDailySnapshot(Account account) {
        LocalDate today = LocalDate.now();
        List<AccountBalanceHistory> todayHistory = historyRepository.findByAccountIdAndDateBetweenOrderByDateAsc(
                account.getId(), today, today);
                
        AccountBalanceHistory history;
        if (!todayHistory.isEmpty()) {
            history = todayHistory.get(0);
            history.setBalance(account.getBalance());
            history.setRecordedAt(java.time.LocalDateTime.now());
        } else {
            history = AccountBalanceHistory.builder()
                    .accountId(account.getId())
                    .date(today)
                    .balance(account.getBalance())
                    .build();
        }
        historyRepository.save(history);
    }
}
