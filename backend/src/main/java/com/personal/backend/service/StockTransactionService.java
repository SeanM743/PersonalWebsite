package com.personal.backend.service;

import com.personal.backend.model.AccountTransaction;
import com.personal.backend.model.StockTransaction;
import com.personal.backend.repository.AccountTransactionRepository;
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
    private final AccountTransactionRepository accountTransactionRepository;
    private final StockHoldingService stockHoldingService;

    public StockTransaction addTransaction(StockTransaction transaction) {
        // 1. Save Transaction
        StockTransaction savedTxn = transactionRepository.save(transaction);
        
        // 2. Update Accounts Logic (Cash Balance)
        // If accountId is specified, use that account. Otherwise fall back to "Fidelity Cash".
        Account cashAccount = null;
        if (transaction.getAccountId() != null) {
            cashAccount = accountRepository.findById(transaction.getAccountId()).orElse(null);
            if (cashAccount == null) {
                log.warn("Account ID {} not found, skipping balance update", transaction.getAccountId());
            }
        } else {
            cashAccount = accountRepository.findByName("Fidelity Cash");
        }
        
        if (cashAccount != null && transaction.getTotalCost() != null) {
            BigDecimal oldBalance = cashAccount.getBalance();
            
            if (transaction.getType() == StockTransaction.TransactionType.BUY) {
                BigDecimal cost = transaction.getTotalCost();
                log.info("Processing BUY: Debiting ${} from {} (ID: {})", cost, cashAccount.getName(), cashAccount.getId());
                BigDecimal newBalance = oldBalance.subtract(cost);
                cashAccount.setBalance(newBalance);
                accountRepository.save(cashAccount);
                
                // Record account transaction
                accountTransactionRepository.save(AccountTransaction.builder()
                        .accountId(cashAccount.getId())
                        .transactionDate(transaction.getTransactionDate())
                        .amount(cost)
                        .oldBalance(oldBalance)
                        .newBalance(newBalance)
                        .type(AccountTransaction.TransactionType.DEBIT)
                        .description("BUY " + transaction.getQuantity().stripTrailingZeros().toPlainString() + " " + transaction.getSymbol() + " @ $" + transaction.getPricePerShare().stripTrailingZeros().toPlainString())
                        .relatedStockTransactionId(savedTxn.getId())
                        .build());
                        
            } else if (transaction.getType() == StockTransaction.TransactionType.SELL) {
                BigDecimal proceeds = transaction.getTotalCost();
                log.info("Processing SELL: Crediting ${} to {} (ID: {})", proceeds, cashAccount.getName(), cashAccount.getId());
                BigDecimal newBalance = oldBalance.add(proceeds);
                cashAccount.setBalance(newBalance);
                accountRepository.save(cashAccount);
                
                // Record account transaction
                accountTransactionRepository.save(AccountTransaction.builder()
                        .accountId(cashAccount.getId())
                        .transactionDate(transaction.getTransactionDate())
                        .amount(proceeds)
                        .oldBalance(oldBalance)
                        .newBalance(newBalance)
                        .type(AccountTransaction.TransactionType.CREDIT)
                        .description("SELL " + transaction.getQuantity().stripTrailingZeros().toPlainString() + " " + transaction.getSymbol() + " @ $" + transaction.getPricePerShare().stripTrailingZeros().toPlainString())
                        .relatedStockTransactionId(savedTxn.getId())
                        .build());
            }
        }
        
        // 3. Recalculate Holdings for accuracy
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
