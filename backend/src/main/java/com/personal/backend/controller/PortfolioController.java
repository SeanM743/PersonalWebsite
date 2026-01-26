package com.personal.backend.controller;

import com.personal.backend.dto.PortfolioResponse;
import com.personal.backend.dto.PortfolioSummary;
import com.personal.backend.dto.CompletePortfolioSummary;
import com.personal.backend.dto.StockRequest;
import com.personal.backend.model.StockTicker;
import com.personal.backend.service.PortfolioService;
import com.personal.backend.service.StockHoldingService;
import com.personal.backend.service.StockTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for portfolio management operations
 */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class PortfolioController {
    
    private final PortfolioService portfolioService;
    private final StockHoldingService stockHoldingService;
    private final StockTransactionService stockTransactionService;
    
    /**
     * Get portfolio summary with real-time market data
     * GET /api/portfolio
     */
    @GetMapping
    public ResponseEntity<PortfolioResponse<PortfolioSummary>> getPortfolio(
            @RequestParam(value = "detailed", defaultValue = "false") boolean detailed,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Getting portfolio for user {} (detailed: {})", userId, detailed);
            
            PortfolioResponse<PortfolioSummary> response = detailed ? 
                    portfolioService.getDetailedPortfolioSummary(userId) :
                    portfolioService.getPortfolioSummary(userId);
            
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : 
                    (response.getError().contains("not found") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST);
            
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            log.error("Error getting portfolio: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PortfolioResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Get complete portfolio including stocks, cash, and retirement accounts
     * GET /api/portfolio/complete
     */
    @GetMapping("/complete")
    public ResponseEntity<PortfolioResponse<CompletePortfolioSummary>> getCompletePortfolio(
            @RequestParam(value = "detailed", defaultValue = "true") boolean detailed,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Getting complete portfolio for user {} (detailed: {})", userId, detailed);
            
            // Get stock portfolio summary
            PortfolioResponse<PortfolioSummary> stockResponse = detailed ? 
                    portfolioService.getDetailedPortfolioSummary(userId) :
                    portfolioService.getPortfolioSummary(userId);
            
            if (!stockResponse.isSuccess()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(PortfolioResponse.error("Failed to get stock portfolio: " + stockResponse.getError()));
            }
            
            // Create complete portfolio with static holdings
            CompletePortfolioSummary completePortfolio = CompletePortfolioSummary.createWithStaticHoldings(
                    stockResponse.getData());
            
            PortfolioResponse<CompletePortfolioSummary> response = PortfolioResponse.success(
                    completePortfolio, 
                    "Complete portfolio retrieved successfully",
                    Map.of(
                        "includesStaticHoldings", true,
                        "totalValue", completePortfolio.getTotalPortfolioValue(),
                        "stockPositions", completePortfolio.getStockPortfolio().getTotalPositions(),
                        "staticPositions", completePortfolio.getStaticHoldings().getHoldings().size()
                    )
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting complete portfolio: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PortfolioResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Refresh portfolio data with latest market information
     * POST /api/portfolio/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<PortfolioResponse<PortfolioSummary>> refreshPortfolio(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Refreshing portfolio for user {}", userId);
            
            PortfolioResponse<PortfolioSummary> response = portfolioService.refreshPortfolioData(userId);
            
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            log.error("Error refreshing portfolio: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PortfolioResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Get portfolio performance for specific time period
     * GET /api/portfolio/performance
     */
    @GetMapping("/performance")
    public ResponseEntity<PortfolioResponse<Map<String, Object>>> getPortfolioPerformance(
            @RequestParam(value = "period", defaultValue = "1d") String period,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Getting portfolio performance for user {} over period {}", userId, period);
            
            PortfolioResponse<Map<String, Object>> response = portfolioService.getPortfolioPerformance(userId, period);
            
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            log.error("Error getting portfolio performance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PortfolioResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Get all stock holdings
     * GET /api/portfolio/holdings
     */
    @GetMapping("/holdings")
    public ResponseEntity<PortfolioResponse<List<StockTicker>>> getHoldings(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Getting holdings for user {}", userId);
            
            PortfolioResponse<List<StockTicker>> response = stockHoldingService.getUserStockHoldings(userId);
            
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            log.error("Error getting holdings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PortfolioResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Add new stock holding
     * POST /api/portfolio/holdings
     */
    @PostMapping("/holdings")
    public ResponseEntity<PortfolioResponse<StockTicker>> addHolding(
            @Valid @RequestBody StockRequest request,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Adding holding for user {}: {}", userId, request.getSymbol());
            
            PortfolioResponse<StockTicker> response = stockHoldingService.addStockHolding(userId, request);
            
            HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : 
                    (response.getError().contains("duplicate") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST);
            
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            log.error("Error adding holding: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PortfolioResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Get specific stock holding
     * GET /api/portfolio/holdings/{symbol}
     */
    @GetMapping("/holdings/{symbol}")
    public ResponseEntity<PortfolioResponse<StockTicker>> getHolding(
            @PathVariable String symbol,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Getting holding for user {} and symbol {}", userId, symbol);
            
            PortfolioResponse<StockTicker> response = stockHoldingService.getStockHolding(userId, symbol);
            
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : 
                    (response.getError().contains("not found") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST);
            
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            log.error("Error getting holding: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PortfolioResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Update existing stock holding
     * PUT /api/portfolio/holdings/{symbol}
     */
    @PutMapping("/holdings/{symbol}")
    public ResponseEntity<PortfolioResponse<StockTicker>> updateHolding(
            @PathVariable String symbol,
            @Valid @RequestBody StockRequest request,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Updating holding for user {} and symbol {}", userId, symbol);
            
            PortfolioResponse<StockTicker> response = stockHoldingService.updateStockHolding(userId, symbol, request);
            
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : 
                    (response.getError().contains("not found") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST);
            
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            log.error("Error updating holding: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PortfolioResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Delete stock holding
     * DELETE /api/portfolio/holdings/{symbol}
     */
    @DeleteMapping("/holdings/{symbol}")
    public ResponseEntity<PortfolioResponse<Boolean>> deleteHolding(
            @PathVariable String symbol,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Deleting holding for user {} and symbol {}", userId, symbol);
            
            PortfolioResponse<Boolean> response = stockHoldingService.removeStockHolding(userId, symbol);
            
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : 
                    (response.getError().contains("not found") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST);
            
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            log.error("Error deleting holding: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PortfolioResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Search stock holdings
     * GET /api/portfolio/holdings/search
     */
    @GetMapping("/holdings/search")
    public ResponseEntity<PortfolioResponse<List<StockTicker>>> searchHoldings(
            @RequestParam("q") String searchTerm,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Searching holdings for user {} with term: {}", userId, searchTerm);
            
            PortfolioResponse<List<StockTicker>> response = stockHoldingService.searchStockHoldings(userId, searchTerm);
            
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            log.error("Error searching holdings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PortfolioResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Get portfolio statistics
     * GET /api/portfolio/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<PortfolioResponse<Map<String, Object>>> getPortfolioStatistics(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Getting portfolio statistics for user {}", userId);
            
            PortfolioResponse<Map<String, Object>> response = stockHoldingService.getPortfolioStatistics(userId);
            
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            log.error("Error getting portfolio statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PortfolioResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Bulk update stock holdings
     * PUT /api/portfolio/holdings/bulk
     */
    @PutMapping("/holdings/bulk")
    public ResponseEntity<PortfolioResponse<Integer>> bulkUpdateHoldings(
            @Valid @RequestBody List<StockRequest> requests,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Bulk updating {} holdings for user {}", requests.size(), userId);
            
            PortfolioResponse<Integer> response = stockHoldingService.bulkUpdateStockHoldings(userId, requests);
            
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            log.error("Error bulk updating holdings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PortfolioResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Get portfolio health check
     * GET /api/portfolio/health
     */
    @GetMapping("/health")
    public ResponseEntity<PortfolioResponse<Map<String, Object>>> getPortfolioHealth(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Getting portfolio health check for user {}", userId);
            
            PortfolioResponse<Map<String, Object>> response = portfolioService.getPortfolioHealthCheck(userId);
            
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            log.error("Error getting portfolio health: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PortfolioResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Extract user ID from authentication
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        
        try {
            // For now, assume the authentication name is the user ID
            // In a real implementation, this would extract from JWT or user details
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            // If authentication name is username, we'd need to look up user ID
            // For demo purposes, return a default user ID
            log.warn("Could not parse user ID from authentication name: {}", authentication.getName());
            return 1L; // Default user ID for demo
        }
    }
    
    /**
     * Global exception handler for this controller
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PortfolioResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Invalid argument: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(PortfolioResponse.validationError(e.getMessage()));
    }
    
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<PortfolioResponse<Object>> handleSecurityException(SecurityException e) {
        log.error("Security error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(PortfolioResponse.error("Access denied: " + e.getMessage()));
    }

    /**
     * Get stock transaction history
     * GET /api/portfolio/transactions
     */
    @GetMapping("/transactions")
    public ResponseEntity<PortfolioResponse<List<com.personal.backend.model.StockTransaction>>> getTransactions(
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("Getting stock transactions for user {}", userId);

            List<com.personal.backend.model.StockTransaction> transactions = stockTransactionService.getTransactions(userId);

            return ResponseEntity.ok(PortfolioResponse.success(transactions, "Transactions retrieved successfully"));

        } catch (Exception e) {
            log.error("Error getting transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PortfolioResponse.error("Internal server error: " + e.getMessage()));
        }
    }


    /**
     * Add new stock transaction
     * POST /api/portfolio/transactions
     */
    @PostMapping("/transactions")
    public ResponseEntity<PortfolioResponse<com.personal.backend.model.StockTransaction>> addTransaction(
            @Valid @RequestBody com.personal.backend.model.StockTransaction transaction,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            transaction.setUserId(userId);
            
            log.info("Adding transaction for user {}: {} {}", userId, transaction.getType(), transaction.getSymbol());
            
            com.personal.backend.model.StockTransaction savedTxn = stockTransactionService.addTransaction(transaction);
            
            return ResponseEntity.ok(PortfolioResponse.success(savedTxn, "Transaction added successfully"));
            
        } catch (Exception e) {
            log.error("Error adding transaction: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PortfolioResponse.error("Internal server error: " + e.getMessage()));
        }
    }
}