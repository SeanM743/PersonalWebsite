package com.personal.backend.controller;

import com.personal.backend.dto.CreatePortfolioRequest;
import com.personal.backend.dto.SandboxPortfolioDetailDTO;
import com.personal.backend.dto.SandboxTradeRequest;
import com.personal.backend.model.SandboxPortfolio;
import com.personal.backend.model.SandboxTransaction;
import com.personal.backend.service.SandboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sandbox")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000") // Allow frontend access
public class SandboxController {

    private final SandboxService sandboxService;

    // TODO: Get userId from SecurityContext
    private final Long DEFAULT_USER_ID = 1L;

    @PostMapping("/portfolios")
    public ResponseEntity<SandboxPortfolio> createPortfolio(@RequestBody CreatePortfolioRequest request) {
        return ResponseEntity.ok(sandboxService.createPortfolio(DEFAULT_USER_ID, request));
    }

    @GetMapping("/portfolios")
    public ResponseEntity<List<SandboxPortfolio>> getPortfolios() {
        return ResponseEntity.ok(sandboxService.getPortfolios(DEFAULT_USER_ID));
    }

    @GetMapping("/portfolios/{id}")
    public ResponseEntity<SandboxPortfolioDetailDTO> getPortfolioDetails(@PathVariable Long id) {
        return ResponseEntity.ok(sandboxService.getPortfolioDetails(id));
    }

    @PostMapping("/portfolios/{id}/transactions")
    public ResponseEntity<SandboxTransaction> executeTrade(@PathVariable Long id, @RequestBody SandboxTradeRequest request) {
        return ResponseEntity.ok(sandboxService.executeTrade(id, request));
    }
    
    @PutMapping("/portfolios/{id}")
    public ResponseEntity<SandboxPortfolio> updatePortfolio(@PathVariable Long id, @RequestBody CreatePortfolioRequest request) {
        return ResponseEntity.ok(sandboxService.updatePortfolio(id, request));
    }
    
    @GetMapping("/price")
    public ResponseEntity<java.math.BigDecimal> getHistoricalPrice(@RequestParam String symbol, @RequestParam String date) {
        return ResponseEntity.ok(sandboxService.getHistoricalPrice(symbol, java.time.LocalDate.parse(date)));
    }

    @DeleteMapping("/portfolios/{id}/transactions/{txnId}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id, @PathVariable Long txnId) {
        sandboxService.deleteTransaction(id, txnId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/portfolios/{id}/transactions/{txnId}")
    public ResponseEntity<SandboxTransaction> editTransaction(
            @PathVariable Long id, @PathVariable Long txnId, @RequestBody SandboxTradeRequest request) {
        return ResponseEntity.ok(sandboxService.editTransaction(id, txnId, request));
    }
}
