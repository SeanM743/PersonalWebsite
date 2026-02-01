package com.personal.backend.service;

import com.personal.backend.model.Account;
import com.personal.backend.model.AccountBalanceHistory;
import com.personal.backend.model.StockTransaction;
import com.personal.backend.repository.AccountBalanceHistoryRepository;
import com.personal.backend.repository.AccountRepository;
import com.personal.backend.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountSnapshotService {
    
    private final AccountRepository accountRepository;
    private final AccountBalanceHistoryRepository historyRepository;
    private final StockTransactionRepository transactionRepository;
    private final YahooFinanceService yahooFinanceService;
    
    /**
     * Create daily snapshots for all accounts for today
     */
    @Transactional
    public void createDailySnapshots() {
        LocalDate today = LocalDate.now();
        createSnapshotsForDate(today);
    }
    
    /**
     * Create snapshots for all accounts for a specific date
     */
    @Transactional
    public void createSnapshotsForDate(LocalDate date) {
        log.info("Creating account snapshots for {}", date);
        
        List<Account> accounts = accountRepository.findAll();
        int created = 0;
        int skipped = 0;
        
        for (Account account : accounts) {
            try {
                BigDecimal balance = calculateAccountBalance(account, date);
                
                AccountBalanceHistory snapshot = AccountBalanceHistory.builder()
                    .accountId(account.getId())
                    .date(date)
                    .balance(balance)
                    .build();
                
                historyRepository.save(snapshot);
                created++;
                log.debug("Created snapshot for account {} ({}): ${}", 
                    account.getId(), account.getName(), balance);
                    
            } catch (DataIntegrityViolationException e) {
                // Snapshot already exists for this date
                skipped++;
                log.debug("Snapshot already exists for account {} on {}", account.getId(), date);
            } catch (Exception e) {
                log.error("Failed to create snapshot for account {} on {}: {}", 
                    account.getId(), date, e.getMessage(), e);
            }
        }
        
        log.info("Created {} snapshots, skipped {} duplicates for {}", created, skipped, date);
    }
    
    /**
     * Calculate the balance for an account on a specific date
     */
    private BigDecimal calculateAccountBalance(Account account, LocalDate date) {
        if ("STOCK_PORTFOLIO".equals(account.getType())) {
            return calculateStockAccountBalance(account, date);
        } else {
            return getManualAccountBalance(account);
        }
    }
    
    /**
     * Calculate stock account balance from holdings and stock prices
     */
    private BigDecimal calculateStockAccountBalance(Account account, LocalDate date) {
        // Get all transactions up to the specified date
        // Note: Accounts are global, so we get all transactions for stock portfolio
        List<StockTransaction> transactions = transactionRepository
            .findAll()
            .stream()
            .filter(txn -> !txn.getTransactionDate().isAfter(date))
            .collect(Collectors.toList());
        
        if (transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Calculate current holdings
        Map<String, BigDecimal> holdings = new HashMap<>();
        for (StockTransaction txn : transactions) {
            holdings.putIfAbsent(txn.getSymbol(), BigDecimal.ZERO);
            
            if (txn.getType() == StockTransaction.TransactionType.BUY) {
                holdings.put(txn.getSymbol(), 
                    holdings.get(txn.getSymbol()).add(txn.getQuantity()));
            } else {
                holdings.put(txn.getSymbol(), 
                    holdings.get(txn.getSymbol()).subtract(txn.getQuantity()));
            }
        }
        
        // Calculate total value using closing prices
        BigDecimal totalValue = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> holding : holdings.entrySet()) {
            String symbol = holding.getKey();
            BigDecimal quantity = holding.getValue();
            
            if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                Optional<BigDecimal> price = yahooFinanceService.getClosingPrice(symbol, date);
                if (price.isPresent()) {
                    totalValue = totalValue.add(quantity.multiply(price.get()));
                } else {
                    log.warn("No price found for {} on {} (quantity: {})", symbol, date, quantity);
                }
            }
        }
        
        return totalValue;
    }
    
    /**
     * Get balance for manual accounts (savings, checking, etc.)
     */
    private BigDecimal getManualAccountBalance(Account account) {
        return account.getBalance();
    }
    
    /**
     * Backfill historical snapshots from startDate to endDate
     */
    @Transactional
    public void backfillHistoricalSnapshots(LocalDate startDate, LocalDate endDate) {
        log.info("Backfilling account snapshots from {} to {}", startDate, endDate);
        
        // First, ensure we have stock price data
        List<StockTransaction> allTransactions = transactionRepository.findAll();
        Set<String> symbols = allTransactions.stream()
            .map(StockTransaction::getSymbol)
            .collect(Collectors.toSet());
        
        if (!symbols.isEmpty()) {
            log.info("Ensuring stock price data exists for {} symbols", symbols.size());
            yahooFinanceService.ensureHistoricalDataExists(new ArrayList<>(symbols));
        }
        
        // Create snapshots for each date
        LocalDate currentDate = startDate;
        int totalDays = 0;
        
        while (!currentDate.isAfter(endDate)) {
            createSnapshotsForDate(currentDate);
            currentDate = currentDate.plusDays(1);
            totalDays++;
        }
        
        log.info("Backfill complete: processed {} days", totalDays);
    }
    
    /**
     * Fill missing snapshots for all accounts
     */
    @Transactional
    public void fillMissingSnapshots() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.now();
        
        log.info("Checking for missing snapshots from {} to {}", startDate, endDate);
        
        List<Account> accounts = accountRepository.findAll();
        int totalFilled = 0;
        
        for (Account account : accounts) {
            List<LocalDate> missingDates = findMissingDates(account.getId(), startDate, endDate);
            
            if (!missingDates.isEmpty()) {
                log.info("Found {} missing dates for account {} ({})", 
                    missingDates.size(), account.getId(), account.getName());
                
                for (LocalDate date : missingDates) {
                    try {
                        BigDecimal balance = calculateAccountBalance(account, date);
                        
                        AccountBalanceHistory snapshot = AccountBalanceHistory.builder()
                            .accountId(account.getId())
                            .date(date)
                            .balance(balance)
                            .build();
                        
                        historyRepository.save(snapshot);
                        totalFilled++;
                    } catch (Exception e) {
                        log.error("Failed to fill snapshot for account {} on {}: {}", 
                            account.getId(), date, e.getMessage());
                    }
                }
            }
        }
        
        log.info("Filled {} missing snapshots", totalFilled);
    }
    
    /**
     * Find dates that are missing snapshots for an account
     */
    private List<LocalDate> findMissingDates(Long accountId, LocalDate startDate, LocalDate endDate) {
        // Get all existing snapshot dates for this account
        List<AccountBalanceHistory> existingSnapshots = historyRepository
            .findByAccountIdAndDateBetweenOrderByDateAsc(accountId, startDate, endDate);
        
        Set<LocalDate> existingDates = existingSnapshots.stream()
            .map(AccountBalanceHistory::getDate)
            .collect(Collectors.toSet());
        
        // Find missing dates
        List<LocalDate> missingDates = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            if (!existingDates.contains(currentDate)) {
                missingDates.add(currentDate);
            }
            currentDate = currentDate.plusDays(1);
        }
        
        return missingDates;
    }
}
