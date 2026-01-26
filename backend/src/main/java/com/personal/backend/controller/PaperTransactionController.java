package com.personal.backend.controller;

import com.personal.backend.dto.AccountResponse;
import com.personal.backend.model.PaperTransaction;
import com.personal.backend.repository.PaperTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/paper-trading")
@RequiredArgsConstructor
@Slf4j
public class PaperTransactionController {

    private final PaperTransactionRepository repository;

    @GetMapping
    public ResponseEntity<AccountResponse<List<PaperTransaction>>> getTransactions() {
        Long userId = 1L; // User Context
        List<PaperTransaction> txns = repository.findByUserIdOrderByTransactionDateDesc(userId);
        return ResponseEntity.ok(AccountResponse.success(txns, "Paper trading history retrieved"));
    }

    @PostMapping
    public ResponseEntity<AccountResponse<PaperTransaction>> addTransaction(@RequestBody @Valid PaperTransaction txn) {
        Long userId = 1L;
        txn.setUserId(userId);
        if (txn.getTransactionDate() == null) {
            txn.setTransactionDate(java.time.LocalDateTime.now());
        }
        
        PaperTransaction saved = repository.save(txn);
        return ResponseEntity.ok(AccountResponse.success(saved, "Paper transaction executed"));
    }
    
    @DeleteMapping("/reset")
    public ResponseEntity<AccountResponse<Void>> resetPortfolio() {
        Long userId = 1L;
        // In real app, delete by user ID
        repository.deleteAll(); 
        return ResponseEntity.ok(AccountResponse.success(null, "Paper portfolio reset"));
    }
}
