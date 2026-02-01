package com.personal.backend.controller;

import com.personal.backend.dto.AccountResponse;
import com.personal.backend.model.Account;
import com.personal.backend.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

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
}
