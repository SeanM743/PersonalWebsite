package com.personal.backend.service;

import com.personal.backend.model.StockTransaction;
import com.personal.backend.repository.StockTransactionRepository;
import com.personal.backend.model.Account;
import com.personal.backend.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockTransactionService {

    private final StockTransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final StockHoldingService stockHoldingService;

    public StockTransaction addTransaction(StockTransaction transaction) {
        // 1. Save Transaction
        StockTransaction savedTxn = transactionRepository.save(transaction);
        
        // 2. Update Accounts Logic (Cash Balance)
        // In a real system, we'd look up specific accounts configured for the user.
        // For this personal app, we assume "Fidelity Cash" and "Main Portfolio" exist.
        
        Account cashAccount = accountRepository.findByName("Fidelity Cash");
        
        if (cashAccount != null && transaction.getTotalCost() != null) {
            if (transaction.getType() == StockTransaction.TransactionType.BUY) {
                // 1. Debit Cash
                BigDecimal cost = transaction.getTotalCost();
                log.info("Processing BUY: Debiting ${} from {}", cost, cashAccount.getName());
                cashAccount.setBalance(cashAccount.getBalance().subtract(cost));
                accountRepository.save(cashAccount);
            } else if (transaction.getType() == StockTransaction.TransactionType.SELL) {
                // 1. Credit Cash
                BigDecimal proceeds = transaction.getTotalCost(); // Assuming totalCost here represents total value of sale
                log.info("Processing SELL: Crediting ${} to {}", proceeds, cashAccount.getName());
                cashAccount.setBalance(cashAccount.getBalance().add(proceeds));
                accountRepository.save(cashAccount);
            }
        }
        
        // 3. Recalculate Holdings for accuracy
        // Instead of incremental updates, we trigger a full recalculation for the user
        // This ensures the Weighted Average Cost is always mathematically correct based on history
        try {
             stockHoldingService.recalculateHoldings(transaction.getUserId());
        } catch (Exception e) {
            log.error("Failed to recalculate holdings after transaction: {}", e.getMessage());
        }
        
        return savedTxn;
    }

    public List<StockTransaction> getTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
    }

    public List<StockTransaction> getTransactionsForSymbol(Long userId, String symbol) {
        return transactionRepository.findByUserIdAndSymbolOrderByTransactionDateDesc(userId, symbol);
    }
}
