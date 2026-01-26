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
        
        // 2. Update Accounts Logic
        // In a real system, we'd look up specific accounts configured for the user.
        // For this personal app, we assume "Fidelity Cash" and "Main Portfolio" exist.
        
        Account cashAccount = accountRepository.findByName("Fidelity Cash");
        Account stockAccount = accountRepository.findByName("Stock Portfolio"); // Or similar name
        
        if (cashAccount != null && stockAccount != null && transaction.getTotalCost() != null) {
            if (transaction.getType() == StockTransaction.TransactionType.BUY) {
                // 1. Debit Cash
                BigDecimal cost = transaction.getTotalCost();
                log.info("Processing BUY: Debiting ${} from {}", cost, cashAccount.getName());
                cashAccount.setBalance(cashAccount.getBalance().subtract(cost));
                accountRepository.save(cashAccount);
                
                // 2. Update Holdings (Add Shares)
                // We use a simplified update here or assume the Service handles "Add/Update" logic.
                // Since StockHoldingService inputs are DTOs, we might need a helper method or direct repository access.
                // For simplicity/cleanliness, let's assume we call a method to update position.
                try {
                     updateHoldingFromTransaction(transaction.getUserId(), transaction);
                } catch (Exception e) {
                    log.error("Failed to update holding for transaction: {}", e.getMessage());
                }

            } else if (transaction.getType() == StockTransaction.TransactionType.SELL) {
                // 1. Credit Cash
                BigDecimal proceeds = transaction.getTotalCost(); // Assuming totalCost here represents total value of sale
                log.info("Processing SELL: Crediting ${} to {}", proceeds, cashAccount.getName());
                cashAccount.setBalance(cashAccount.getBalance().add(proceeds));
                accountRepository.save(cashAccount);

                // 2. Update Holdings (Reduce Shares)
                try {
                     updateHoldingFromTransaction(transaction.getUserId(), transaction);
                } catch (Exception e) {
                    log.error("Failed to update holding for transaction: {}", e.getMessage());
                }
            }
        }
        
        return savedTxn;
    }

    public List<StockTransaction> getTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
    }

    public List<StockTransaction> getTransactionsForSymbol(Long userId, String symbol) {
        return transactionRepository.findByUserIdAndSymbolOrderByTransactionDateDesc(userId, symbol);
    }

    private void updateHoldingFromTransaction(Long userId, StockTransaction txn) {
        // This is a naive implementation. In a real app we'd need robust Cost Basis tracking (FIFO/LIFO).
        // Here we just update Weighted Average Cost for BUY, and reduce quantity for SELL.
        
        com.personal.backend.dto.PortfolioResponse<com.personal.backend.model.StockTicker> holdingResp = 
            stockHoldingService.getStockHolding(userId, txn.getSymbol());
            
        if (holdingResp.isSuccess()) {
             // Update existing
             com.personal.backend.model.StockTicker existing = holdingResp.getData();
             BigDecimal newQty;
             BigDecimal newAvgPrice = existing.getPurchasePrice();

             if (txn.getType() == StockTransaction.TransactionType.BUY) {
                 BigDecimal totalValueOld = existing.getQuantity().multiply(existing.getPurchasePrice());
                 BigDecimal totalValueNew = txn.getTotalCost();
                 BigDecimal totalQty = existing.getQuantity().add(txn.getQuantity());
                 
                 newQty = totalQty;
                 if (totalQty.compareTo(BigDecimal.ZERO) > 0) {
                    newAvgPrice = totalValueOld.add(totalValueNew).divide(totalQty, 4, java.math.RoundingMode.HALF_UP);
                 }
             } else {
                 // SELL: Price doesn't change average cost, just quantity reduces
                 newQty = existing.getQuantity().subtract(txn.getQuantity());
             }

             // Update via service
             com.personal.backend.dto.StockRequest req = new com.personal.backend.dto.StockRequest();
             req.setSymbol(txn.getSymbol());
             req.setQuantity(newQty);
             req.setPurchasePrice(newAvgPrice); 
             req.setNotes(existing.getNotes());
             
             if (newQty.compareTo(BigDecimal.ZERO) <= 0) {
                 stockHoldingService.removeStockHolding(userId, txn.getSymbol());
             } else {
                 stockHoldingService.updateStockHolding(userId, txn.getSymbol(), req);
             }

        } else {
            // New Position (Only valid for BUY)
            if (txn.getType() == StockTransaction.TransactionType.BUY) {
                 com.personal.backend.dto.StockRequest req = new com.personal.backend.dto.StockRequest();
                 req.setSymbol(txn.getSymbol());
                 req.setQuantity(txn.getQuantity());
                 req.setPurchasePrice(txn.getPricePerShare());
                 req.setNotes("Opened via Transaction");
                 stockHoldingService.addStockHolding(userId, req);
            } else {
                log.warn("Attempted to SELL a position that doesn't exist: {}", txn.getSymbol());
            }
        }
    }
}
