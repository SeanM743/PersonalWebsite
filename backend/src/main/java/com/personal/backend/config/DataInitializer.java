package com.personal.backend.config;

import com.personal.backend.dto.StockRequest;
import com.personal.backend.model.Role;
import com.personal.backend.service.AuthenticationService;
import com.personal.backend.service.StockHoldingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final AuthenticationService authenticationService;
    private final StockHoldingService stockHoldingService;
    
    @Override
    public void run(String... args) throws Exception {
        initializeDefaultUsers();
        initializePortfolioStocks();
    }
    
    private void initializeDefaultUsers() {
        try {
            // Create or update default admin user
            if (!authenticationService.userExists("admin")) {
                authenticationService.createUser("admin", "default123", Role.ADMIN);
                log.info("Created default admin user");
            } else {
                // Update existing admin user password
                try {
                    authenticationService.updateUserPassword("admin", "default123");
                    log.info("Updated admin user password");
                } catch (Exception e) {
                    log.warn("Could not update admin password: {}", e.getMessage());
                }
                log.info("Default admin user already exists");
            }
            
            // Create or update default guest user
            if (!authenticationService.userExists("guest")) {
                authenticationService.createUser("guest", "default123", Role.GUEST);
                log.info("Created default guest user");
            } else {
                // Update existing guest user password
                try {
                    authenticationService.updateUserPassword("guest", "default123");
                    log.info("Updated guest user password");
                } catch (Exception e) {
                    log.warn("Could not update guest password: {}", e.getMessage());
                }
                log.info("Default guest user already exists");
            }
            
        } catch (Exception e) {
            log.error("Error initializing default users: {}", e.getMessage(), e);
        }
    }
    
    private void initializePortfolioStocks() {
        try {
            Long adminUserId = 1L; // Admin user ID
            
            // Define your stock positions
            StockPosition[] positions = {
                new StockPosition("AMZN", new BigDecimal("150.00"), new BigDecimal("10"), "Amazon - E-commerce and cloud computing giant"),
                new StockPosition("SOFI", new BigDecimal("8.50"), new BigDecimal("100"), "SoFi Technologies - Digital financial services"),
                new StockPosition("ANET", new BigDecimal("320.00"), new BigDecimal("5"), "Arista Networks - Cloud networking solutions"),
                new StockPosition("INTC", new BigDecimal("25.00"), new BigDecimal("50"), "Intel Corporation - Semiconductor manufacturer"),
                new StockPosition("CRWV", new BigDecimal("15.00"), new BigDecimal("25"), "Crown Electrokinetics - Smart glass technology")
            };
            
            for (StockPosition position : positions) {
                try {
                    // Check if stock already exists for this user
                    var existingStock = stockHoldingService.getStockHolding(adminUserId, position.symbol);
                    if (!existingStock.isSuccess()) {
                        // Stock doesn't exist, add it
                        StockRequest request = new StockRequest();
                        request.setSymbol(position.symbol);
                        request.setPurchasePrice(position.purchasePrice);
                        request.setQuantity(position.quantity);
                        request.setNotes(position.notes);
                        
                        var result = stockHoldingService.addStockHolding(adminUserId, request);
                        if (result.isSuccess()) {
                            log.info("Added stock position: {} - {} shares at ${}", 
                                    position.symbol, position.quantity, position.purchasePrice);
                        } else {
                            log.warn("Failed to add stock position {}: {}", position.symbol, result.getError());
                        }
                    } else {
                        log.info("Stock position {} already exists", position.symbol);
                    }
                } catch (Exception e) {
                    log.error("Error adding stock position {}: {}", position.symbol, e.getMessage());
                }
            }
            
            log.info("Portfolio initialization completed");
            
        } catch (Exception e) {
            log.error("Error initializing portfolio stocks: {}", e.getMessage(), e);
        }
    }
    
    private static class StockPosition {
        final String symbol;
        final BigDecimal purchasePrice;
        final BigDecimal quantity;
        final String notes;
        
        StockPosition(String symbol, BigDecimal purchasePrice, BigDecimal quantity, String notes) {
            this.symbol = symbol;
            this.purchasePrice = purchasePrice;
            this.quantity = quantity;
            this.notes = notes;
        }
    }
}