package com.personal.backend.controller;

import com.personal.backend.dto.AccountResponse;
import com.personal.backend.model.Account;
import com.personal.backend.model.AccountTransaction;
import com.personal.backend.model.StockTransaction;
import com.personal.backend.repository.AccountTransactionRepository;
import com.personal.backend.repository.StockTransactionRepository;
import com.personal.backend.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final StockTransactionRepository stockTransactionRepository;
    private final AccountTransactionRepository accountTransactionRepository;

    @GetMapping
    public ResponseEntity<AccountResponse<List<Account>>> getAllAccounts() {
        return ResponseEntity.ok(AccountResponse.success(accountService.getAllAccounts(), "Accounts retrieved successfully"));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse<Account>> getAccount(@PathVariable Long id) {
        return accountService.getAccount(id)
                .map(account -> ResponseEntity.ok(AccountResponse.success(account, "Account retrieved successfully")))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<AccountResponse<List<AccountTransaction>>> getAccountTransactions(@PathVariable Long id) {
        List<AccountTransaction> txns = accountTransactionRepository.findByAccountIdOrderByTransactionDateDescCreatedAtDesc(id);
        return ResponseEntity.ok(AccountResponse.success(txns, "Account transactions retrieved successfully"));
    }

    @PutMapping("/{id}/balance")
    public ResponseEntity<AccountResponse<Account>> updateBalance(@PathVariable Long id, @RequestBody Map<String, BigDecimal> payload) {
        BigDecimal newBalance = payload.get("balance");
        if (newBalance == null) {
            return ResponseEntity.badRequest().body(AccountResponse.error("Balance is required"));
        }
        
        try {
            Account updated = accountService.updateBalance(id, newBalance);
            return ResponseEntity.ok(AccountResponse.success(updated, "Balance updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse<Account>> updateAccount(@PathVariable Long id, @RequestBody Account account) {
        try {
            Account updated = accountService.updateAccount(id, account);
            return ResponseEntity.ok(AccountResponse.success(updated, "Account updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AccountResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<AccountResponse<Account>> createAccount(@RequestBody Account account) {
        try {
            Account created = accountService.createAccount(account);
            return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                    .body(AccountResponse.success(created, "Account created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(AccountResponse.error("Failed to create account: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AccountResponse<Void>> deleteAccount(@PathVariable Long id) {
        try {
            accountService.deleteAccount(id);
            return ResponseEntity.ok(AccountResponse.success(null, "Account deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AccountResponse.error(e.getMessage()));
        }
    }

    /**
     * One-time backfill: Debit the specified account for the last 2 NVO and IGV BUY transactions.
     * POST /api/accounts/backfill-cash?accountId=X
     */
    @PostMapping("/backfill-cash")
    public ResponseEntity<AccountResponse<String>> backfillCash(@RequestParam Long accountId) {
        try {
            Account account = accountService.getAccount(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

            Long userId = 1L;
            List<String> results = new ArrayList<>();
            BigDecimal totalDebited = BigDecimal.ZERO;

            for (String symbol : new String[]{"NVO", "IGV"}) {
                List<StockTransaction> txns = stockTransactionRepository
                        .findByUserIdAndSymbolOrderByTransactionDateDesc(userId, symbol);
                
                // Take the last 2 BUY transactions
                int count = 0;
                for (StockTransaction txn : txns) {
                    if (txn.getType() == StockTransaction.TransactionType.BUY && count < 2) {
                        BigDecimal cost = txn.getTotalCost() != null ? txn.getTotalCost()
                                : txn.getQuantity().multiply(txn.getPricePerShare());
                        
                        account.setBalance(account.getBalance().subtract(cost));
                        
                        // Tag the transaction with the account
                        txn.setAccountId(accountId);
                        stockTransactionRepository.save(txn);
                        
                        results.add(String.format("%s BUY on %s: -$%s", symbol, txn.getTransactionDate(), cost));
                        totalDebited = totalDebited.add(cost);
                        count++;
                    }
                }
            }

            accountService.updateBalance(accountId, account.getBalance());
            
            String summary = String.format("Debited $%s from %s for %d transactions: %s",
                    totalDebited, account.getName(), results.size(), String.join("; ", results));
            log.info("Backfill completed: {}", summary);
            
            return ResponseEntity.ok(AccountResponse.success(summary, "Backfill completed successfully"));
        } catch (Exception e) {
            log.error("Backfill failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(AccountResponse.error("Backfill failed: " + e.getMessage()));
        }
    }
}
