package com.personal.backend.service;

import com.personal.backend.dto.PortfolioHistoryPoint;
import com.personal.backend.model.Account;
import com.personal.backend.model.AccountBalanceHistory;
import com.personal.backend.repository.AccountBalanceHistoryRepository;
import com.personal.backend.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HistoricalPortfolioService {
    
    private final AccountRepository accountRepository;
    private final AccountBalanceHistoryRepository historyRepository;
    private final AccountSnapshotService accountSnapshotService;
    
    /**
     * Get portfolio history for a user over a specified period.
     * Reads from pre-computed snapshots in account_balance_history table.
     */
    public Mono<List<PortfolioHistoryPoint>> getReconstructedHistory(Long userId, String period) {
        return Mono.fromCallable(() -> getReconstructedHistorySync(userId, period));
    }
    
    /**
     * Synchronous version of getReconstructedHistory - avoids reactive overhead.
     * Call this directly when you don't need reactive semantics.
     */
    public List<PortfolioHistoryPoint> getReconstructedHistorySync(Long userId, String period) {
        log.info("Getting portfolio history for user {} with period {}", userId, period);
        
        // 1. Determine date range
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = calculateStartDate(period, endDate);
        
        // Ensure we don't go before Jan 1, 2026
        LocalDate earliestDate = LocalDate.of(2026, 1, 1);
        if (startDate.isBefore(earliestDate)) {
            startDate = earliestDate;
        }
        
        // 2. Get stock portfolio account
        List<Account> accounts = accountRepository.findByType(Account.AccountType.STOCK_PORTFOLIO);
        if (accounts.isEmpty()) {
            log.warn("No stock account found");
            return Collections.emptyList();
        }
        Account stockAccount = accounts.get(0); // Assuming single stock portfolio
        
        // 3. Check if we need to fill missing snapshots
        List<AccountBalanceHistory> existingSnapshots = historyRepository
            .findByAccountIdAndDateBetweenOrderByDateAsc(stockAccount.getId(), startDate, endDate);
        
        // Calculate expected number of days
        long expectedDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        if (existingSnapshots.size() < expectedDays) {
            log.info("Missing snapshots detected. Expected {}, found {}. Filling gaps...", 
                expectedDays, existingSnapshots.size());
            
            // Fill missing snapshots for this account
            accountSnapshotService.fillMissingSnapshots();
            
            // Re-query to get complete data
            existingSnapshots = historyRepository
                .findByAccountIdAndDateBetweenOrderByDateAsc(stockAccount.getId(), startDate, endDate);
        }
        
        // 4. Convert to PortfolioHistoryPoint
        List<PortfolioHistoryPoint> history = existingSnapshots.stream()
            .map(h -> new PortfolioHistoryPoint(h.getDate(), h.getBalance()))
            .collect(Collectors.toList());
        
        log.info("Returning {} portfolio history points from {} to {}", 
            history.size(), startDate, endDate);
        
        return history;
    }
    
    /**
     * Calculate start date based on period string
     */
    private LocalDate calculateStartDate(String period, LocalDate endDate) {
        return switch (period.toUpperCase()) {
            case "1D" -> endDate.minusDays(1);
            case "3D" -> endDate.minusDays(3);
            case "5D" -> endDate.minusDays(5);
            case "1M" -> endDate.minusMonths(1);
            case "3M" -> endDate.minusMonths(3);
            case "6M" -> endDate.minusMonths(6);
            case "YTD" -> LocalDate.of(endDate.getYear(), 1, 1);
            case "1Y" -> endDate.minusYears(1);
            case "3Y" -> endDate.minusYears(3);
            case "5Y" -> endDate.minusYears(5);
            case "ALL" -> LocalDate.of(2026, 1, 1); // Start from Jan 1, 2026
            default -> endDate.minusMonths(1);
        };
    }
}
