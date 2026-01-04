package com.personal.backend.integration;

import com.personal.backend.dto.PortfolioResponse;
import com.personal.backend.dto.PortfolioSummary;
import com.personal.backend.dto.StockRequest;
import com.personal.backend.model.StockTicker;
import com.personal.backend.service.PortfolioService;
import com.personal.backend.service.StockHoldingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Portfolio Dashboard functionality
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PortfolioIntegrationTest {
    
    @Autowired
    private PortfolioService portfolioService;
    
    @Autowired
    private StockHoldingService stockHoldingService;
    
    @Test
    public void testEmptyPortfolioSummary() {
        // Test getting portfolio summary for user with no holdings
        Long userId = 999L;
        
        PortfolioResponse<PortfolioSummary> response = portfolioService.getPortfolioSummary(userId);
        
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(userId, response.getData().getUserId());
        assertEquals(0, response.getData().getTotalPositions());
        assertEquals(BigDecimal.ZERO, response.getData().getTotalInvestment());
        assertEquals(BigDecimal.ZERO, response.getData().getCurrentValue());
    }
    
    @Test
    public void testAddStockHoldingAndGetPortfolio() {
        // Test adding a stock holding and retrieving portfolio
        Long userId = 998L;
        
        // Add a stock holding
        StockRequest request = new StockRequest();
        request.setSymbol("AAPL");
        request.setPurchasePrice(new BigDecimal("150.00"));
        request.setQuantity(new BigDecimal("10"));
        request.setNotes("Test holding");
        
        PortfolioResponse<StockTicker> addResponse = stockHoldingService.addStockHolding(userId, request);
        assertTrue(addResponse.isSuccess());
        assertNotNull(addResponse.getData());
        assertEquals("AAPL", addResponse.getData().getSymbol());
        
        // Get portfolio summary
        PortfolioResponse<PortfolioSummary> summaryResponse = portfolioService.getPortfolioSummary(userId);
        assertTrue(summaryResponse.isSuccess());
        assertNotNull(summaryResponse.getData());
        assertEquals(1, summaryResponse.getData().getTotalPositions());
        assertEquals(new BigDecimal("1500.00"), summaryResponse.getData().getTotalInvestment());
    }
    
    @Test
    public void testStockHoldingValidation() {
        // Test validation of stock holding requests
        Long userId = 997L;
        
        // Test invalid symbol
        StockRequest invalidSymbolRequest = new StockRequest();
        invalidSymbolRequest.setSymbol("invalid_symbol");
        invalidSymbolRequest.setPurchasePrice(new BigDecimal("100.00"));
        invalidSymbolRequest.setQuantity(new BigDecimal("10"));
        
        PortfolioResponse<StockTicker> response = stockHoldingService.addStockHolding(userId, invalidSymbolRequest);
        assertFalse(response.isSuccess());
        assertTrue(response.getError().contains("validation") || response.getError().contains("Invalid"));
        
        // Test negative price
        StockRequest negativePriceRequest = new StockRequest();
        negativePriceRequest.setSymbol("AAPL");
        negativePriceRequest.setPurchasePrice(new BigDecimal("-100.00"));
        negativePriceRequest.setQuantity(new BigDecimal("10"));
        
        response = stockHoldingService.addStockHolding(userId, negativePriceRequest);
        assertFalse(response.isSuccess());
        
        // Test zero quantity
        StockRequest zeroQuantityRequest = new StockRequest();
        zeroQuantityRequest.setSymbol("AAPL");
        zeroQuantityRequest.setPurchasePrice(new BigDecimal("100.00"));
        zeroQuantityRequest.setQuantity(BigDecimal.ZERO);
        
        response = stockHoldingService.addStockHolding(userId, zeroQuantityRequest);
        assertFalse(response.isSuccess());
    }
    
    @Test
    public void testUpdateStockHolding() {
        // Test updating an existing stock holding
        Long userId = 996L;
        
        // Add initial holding
        StockRequest initialRequest = new StockRequest();
        initialRequest.setSymbol("MSFT");
        initialRequest.setPurchasePrice(new BigDecimal("200.00"));
        initialRequest.setQuantity(new BigDecimal("5"));
        
        PortfolioResponse<StockTicker> addResponse = stockHoldingService.addStockHolding(userId, initialRequest);
        assertTrue(addResponse.isSuccess());
        
        // Update the holding
        StockRequest updateRequest = new StockRequest();
        updateRequest.setSymbol("MSFT");
        updateRequest.setPurchasePrice(new BigDecimal("220.00"));
        updateRequest.setQuantity(new BigDecimal("8"));
        updateRequest.setNotes("Updated holding");
        
        PortfolioResponse<StockTicker> updateResponse = stockHoldingService.updateStockHolding(userId, "MSFT", updateRequest);
        assertTrue(updateResponse.isSuccess());
        assertEquals(new BigDecimal("220.00"), updateResponse.getData().getPurchasePrice());
        assertEquals(new BigDecimal("8"), updateResponse.getData().getQuantity());
        assertEquals("Updated holding", updateResponse.getData().getNotes());
    }
    
    @Test
    public void testRemoveStockHolding() {
        // Test removing a stock holding
        Long userId = 995L;
        
        // Add holding
        StockRequest request = new StockRequest();
        request.setSymbol("GOOGL");
        request.setPurchasePrice(new BigDecimal("2500.00"));
        request.setQuantity(new BigDecimal("2"));
        
        PortfolioResponse<StockTicker> addResponse = stockHoldingService.addStockHolding(userId, request);
        assertTrue(addResponse.isSuccess());
        
        // Remove holding
        PortfolioResponse<Boolean> removeResponse = stockHoldingService.removeStockHolding(userId, "GOOGL");
        assertTrue(removeResponse.isSuccess());
        assertTrue(removeResponse.getData());
        
        // Verify it's gone
        PortfolioResponse<StockTicker> getResponse = stockHoldingService.getStockHolding(userId, "GOOGL");
        assertFalse(getResponse.isSuccess());
        assertTrue(getResponse.getError().contains("not found"));
    }
    
    @Test
    public void testPortfolioStatistics() {
        // Test portfolio statistics calculation
        Long userId = 994L;
        
        // Add multiple holdings
        String[] symbols = {"AAPL", "MSFT", "GOOGL"};
        BigDecimal[] prices = {new BigDecimal("150.00"), new BigDecimal("300.00"), new BigDecimal("2500.00")};
        BigDecimal[] quantities = {new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("2")};
        
        for (int i = 0; i < symbols.length; i++) {
            StockRequest request = new StockRequest();
            request.setSymbol(symbols[i]);
            request.setPurchasePrice(prices[i]);
            request.setQuantity(quantities[i]);
            
            PortfolioResponse<StockTicker> response = stockHoldingService.addStockHolding(userId, request);
            assertTrue(response.isSuccess());
        }
        
        // Get portfolio statistics
        PortfolioResponse<java.util.Map<String, Object>> statsResponse = stockHoldingService.getPortfolioStatistics(userId);
        assertTrue(statsResponse.isSuccess());
        
        java.util.Map<String, Object> stats = statsResponse.getData();
        assertEquals(3, stats.get("totalPositions"));
        
        BigDecimal totalInvestment = (BigDecimal) stats.get("totalInvestment");
        BigDecimal expectedTotal = new BigDecimal("1500.00") // AAPL
                .add(new BigDecimal("1500.00")) // MSFT
                .add(new BigDecimal("5000.00")); // GOOGL
        assertEquals(expectedTotal, totalInvestment);
    }
}