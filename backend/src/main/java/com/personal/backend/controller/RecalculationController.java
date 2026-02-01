package com.personal.backend.controller;

import com.personal.backend.dto.PortfolioResponse;
import com.personal.backend.service.StockHoldingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio/recalculate")
@RequiredArgsConstructor
@Slf4j
public class RecalculationController {

    private final StockHoldingService stockHoldingService;

    @PostMapping
    public ResponseEntity<PortfolioResponse<Integer>> recalculatePortfolio(
            @RequestParam(required = false, defaultValue = "1") Long userId) {
        
        log.info("Received request to recalculate portfolio for user {}", userId);
        PortfolioResponse<Integer> response = stockHoldingService.recalculateHoldings(userId);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
