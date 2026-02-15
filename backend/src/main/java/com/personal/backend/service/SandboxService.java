package com.personal.backend.service;

import com.personal.backend.dto.CreatePortfolioRequest;
import com.personal.backend.dto.MarketData;
import com.personal.backend.dto.SandboxHoldingDTO;
import com.personal.backend.dto.SandboxPortfolioDetailDTO;
import com.personal.backend.dto.SandboxTradeRequest;
import com.personal.backend.model.SandboxHolding;
import com.personal.backend.model.SandboxPortfolio;
import com.personal.backend.model.SandboxTransaction;
import com.personal.backend.repository.SandboxHoldingRepository;
import com.personal.backend.repository.SandboxPortfolioRepository;
import com.personal.backend.repository.SandboxTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SandboxService {

    private final SandboxPortfolioRepository portfolioRepository;
    private final SandboxHoldingRepository holdingRepository;
    private final SandboxTransactionRepository transactionRepository;
    private final YahooFinanceService yahooFinanceService;

    public SandboxPortfolio createPortfolio(Long userId, CreatePortfolioRequest request) {
        SandboxPortfolio portfolio = SandboxPortfolio.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .initialBalance(request.getInitialBalance())
                .currentBalance(request.getInitialBalance())
                .build();
        return portfolioRepository.save(portfolio);
    }

    public List<SandboxPortfolio> getPortfolios(Long userId) {
        return portfolioRepository.findByUserId(userId);
    }

    public SandboxPortfolio getPortfolio(Long id) {
        return portfolioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + id));
    }

    public SandboxPortfolioDetailDTO getPortfolioDetails(Long id) {
        SandboxPortfolio portfolio = getPortfolio(id);
        List<SandboxHolding> holdings = holdingRepository.findByPortfolioId(id);
        
        // Fetch current prices
        List<String> symbols = holdings.stream()
                .map(SandboxHolding::getSymbol)
                .collect(Collectors.toList());
        
        Map<String, MarketData> marketDataMap = yahooFinanceService.getBatchMarketData(symbols);
        
        List<SandboxHoldingDTO> holdingDTOs = new ArrayList<>();
        BigDecimal holdingsValue = BigDecimal.ZERO;
        
        for (SandboxHolding holding : holdings) {
            MarketData data = marketDataMap.get(holding.getSymbol());
            BigDecimal currentPrice = data != null ? data.getCurrentPrice() : BigDecimal.ZERO;
            
            BigDecimal marketValue = holding.getQuantity().multiply(currentPrice);
            holdingsValue = holdingsValue.add(marketValue);
            
            BigDecimal totalCost = holding.getQuantity().multiply(holding.getAverageCost());
            BigDecimal gainLoss = marketValue.subtract(totalCost);
            BigDecimal gainLossPct = BigDecimal.ZERO;
            
            if (totalCost.compareTo(BigDecimal.ZERO) != 0) {
                gainLossPct = gainLoss.divide(totalCost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            
            holdingDTOs.add(SandboxHoldingDTO.builder()
                    .id(holding.getId())
                    .symbol(holding.getSymbol())
                    .quantity(holding.getQuantity())
                    .averageCost(holding.getAverageCost())
                    .currentPrice(currentPrice)
                    .marketValue(marketValue)
                    .totalGainLoss(gainLoss)
                    .totalGainLossPercentage(gainLossPct)
                    .build());
        }
        
        BigDecimal totalValue = portfolio.getCurrentBalance().add(holdingsValue);
        BigDecimal totalGainLoss = totalValue.subtract(portfolio.getInitialBalance());
        BigDecimal totalGainLossPct = BigDecimal.ZERO;
        
        if (portfolio.getInitialBalance().compareTo(BigDecimal.ZERO) != 0) {
            totalGainLossPct = totalGainLoss.divide(portfolio.getInitialBalance(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        List<SandboxTransaction> transactions = transactionRepository.findByPortfolioIdOrderByTransactionDateDesc(id);
        
        return SandboxPortfolioDetailDTO.builder()
                .id(portfolio.getId())
                .name(portfolio.getName())
                .description(portfolio.getDescription())
                .initialBalance(portfolio.getInitialBalance())
                .currentBalance(portfolio.getCurrentBalance())
                .holdingsValue(holdingsValue)
                .totalValue(totalValue)
                .totalGainLoss(totalGainLoss)
                .totalGainLossPercentage(totalGainLossPct)
                .createdAt(portfolio.getCreatedAt())
                .holdings(holdingDTOs)
                .recentTransactions(transactions)
                .build();
    }

    public SandboxTransaction executeTrade(Long portfolioId, SandboxTradeRequest request) {
        SandboxPortfolio portfolio = getPortfolio(portfolioId);
        
        BigDecimal price = request.getPrice();
        
        // If price is not provided, try to fetch current market price
        if (price == null) {
            MarketData data = yahooFinanceService.getMarketData(request.getSymbol()).orElse(null);
            if (data != null && data.getCurrentPrice() != null) {
                price = data.getCurrentPrice();
            } else {
                 throw new IllegalArgumentException("Could not determine price for symbol: " + request.getSymbol());
            }
        }

        // Handle dollar-amount mode: calculate quantity from dollarAmount / price
        BigDecimal quantity = request.getQuantity();
        if (quantity == null && request.getDollarAmount() != null) {
            if (price.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("Cannot calculate shares: price is zero for " + request.getSymbol());
            }
            quantity = request.getDollarAmount().divide(price, 8, RoundingMode.HALF_UP);
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Either quantity or dollarAmount must be provided and positive");
        }

        BigDecimal totalCost = quantity.multiply(price);
        
        if (request.getType() == SandboxTransaction.TransactionType.BUY) {
            // Check sufficient funds
            if (portfolio.getCurrentBalance().compareTo(totalCost) < 0) {
                throw new IllegalArgumentException("Insufficient funds for trade. Required: $" + totalCost + ", Available: $" + portfolio.getCurrentBalance());
            }
            // Deduct cash
            portfolio.setCurrentBalance(portfolio.getCurrentBalance().subtract(totalCost));
            
            // Update/Create Holding
            SandboxHolding holding = holdingRepository.findByPortfolioIdAndSymbol(portfolioId, request.getSymbol())
                    .orElse(SandboxHolding.builder()
                            .portfolio(portfolio)
                            .symbol(request.getSymbol())
                            .quantity(BigDecimal.ZERO)
                            .averageCost(BigDecimal.ZERO)
                            .build());
                            
            // Recalculate Average Cost: (OldQty * OldAvg + NewQty * NewPrice) / (OldQty + NewQty)
            BigDecimal currentTotalValue = holding.getQuantity().multiply(holding.getAverageCost());
            BigDecimal tradeValue = quantity.multiply(price);
            BigDecimal newTotalValue = currentTotalValue.add(tradeValue);
            
            BigDecimal newQuantity = holding.getQuantity().add(quantity);
            BigDecimal newAvgCost = BigDecimal.ZERO;
            if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
                 newAvgCost = newTotalValue.divide(newQuantity, 4, RoundingMode.HALF_UP);
            }
            
            holding.setQuantity(newQuantity);
            holding.setAverageCost(newAvgCost);
            holdingRepository.save(holding);
            
        } else if (request.getType() == SandboxTransaction.TransactionType.SELL) {
            // Check sufficient quantity
            SandboxHolding holding = holdingRepository.findByPortfolioIdAndSymbol(portfolioId, request.getSymbol())
                    .orElseThrow(() -> new IllegalArgumentException("Cannot sell symbol not owned: " + request.getSymbol()));
            
            if (holding.getQuantity().compareTo(quantity) < 0) {
                throw new IllegalArgumentException("Insufficient quantity to sell. Owned: " + holding.getQuantity());
            }
            
            // Add cash
            portfolio.setCurrentBalance(portfolio.getCurrentBalance().add(totalCost));
            
            // Update Holding
            BigDecimal newQuantity = holding.getQuantity().subtract(quantity);
            if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
                holdingRepository.delete(holding);
            } else {
                holding.setQuantity(newQuantity);
                // Average cost doesn't change on SELL
                holdingRepository.save(holding);
            }
        }
        
        portfolioRepository.save(portfolio);
        
            // Record Transaction
        SandboxTransaction txn = SandboxTransaction.builder()
                .portfolio(portfolio)
                .symbol(request.getSymbol())
                .type(request.getType())
                .quantity(quantity)
                .price(price)
                .totalCost(totalCost)
                .transactionDate(request.getDate() != null ? request.getDate() : LocalDate.now())
                .build();
                
        return transactionRepository.save(txn);
    }

    public SandboxPortfolio updatePortfolio(Long id, CreatePortfolioRequest request) {
        SandboxPortfolio portfolio = getPortfolio(id);
        
        // If initial balance changes, we need to adjust current balance by the difference
        BigDecimal oldInitial = portfolio.getInitialBalance();
        BigDecimal newInitial = request.getInitialBalance();
        
        if (newInitial.compareTo(oldInitial) != 0) {
            BigDecimal diff = newInitial.subtract(oldInitial);
            portfolio.setCurrentBalance(portfolio.getCurrentBalance().add(diff));
        }
        
        portfolio.setName(request.getName());
        portfolio.setDescription(request.getDescription());
        portfolio.setInitialBalance(newInitial);
        
        return portfolioRepository.save(portfolio);
    }
    
    public BigDecimal getHistoricalPrice(String symbol, LocalDate date) {
        // Ensure data exists
        yahooFinanceService.fetchAndPersistHistoricalPrices(symbol, date, date);
        
        return yahooFinanceService.getClosingPrice(symbol, date)
                .orElseThrow(() -> new IllegalArgumentException("Could not find price for " + symbol + " on " + date));
    }

    /**
     * Delete a transaction and recalculate portfolio balances and holdings.
     * Replays all remaining transactions for the affected symbol to ensure correct avg cost.
     */
    public void deleteTransaction(Long portfolioId, Long transactionId) {
        SandboxTransaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        SandboxPortfolio portfolio = getPortfolio(portfolioId);
        String symbol = txn.getSymbol();

        // Reverse the cash effect
        if (txn.getType() == SandboxTransaction.TransactionType.BUY) {
            portfolio.setCurrentBalance(portfolio.getCurrentBalance().add(txn.getTotalCost()));
        } else {
            portfolio.setCurrentBalance(portfolio.getCurrentBalance().subtract(txn.getTotalCost()));
        }
        portfolioRepository.save(portfolio);

        // Delete the transaction
        transactionRepository.delete(txn);

        // Recalculate holding for this symbol by replaying remaining transactions
        recalculateHolding(portfolioId, symbol, portfolio);
    }

    /**
     * Edit a transaction by reversing it, deleting it, and creating a new one.
     */
    public SandboxTransaction editTransaction(Long portfolioId, Long transactionId, SandboxTradeRequest request) {
        // First delete (reverse) the old transaction
        deleteTransaction(portfolioId, transactionId);

        // Then execute the new trade
        return executeTrade(portfolioId, request);
    }

    /**
     * Recalculate a holding from scratch by replaying all transactions for a symbol.
     */
    private void recalculateHolding(Long portfolioId, String symbol, SandboxPortfolio portfolio) {
        List<SandboxTransaction> remaining = transactionRepository.findByPortfolioIdOrderByTransactionDateDesc(portfolioId)
                .stream()
                .filter(t -> t.getSymbol().equals(symbol))
                .sorted((a, b) -> a.getTransactionDate().compareTo(b.getTransactionDate())) // chronological
                .collect(Collectors.toList());

        // Delete existing holding
        holdingRepository.findByPortfolioIdAndSymbol(portfolioId, symbol)
                .ifPresent(holdingRepository::delete);

        if (remaining.isEmpty()) {
            return; // No more transactions for this symbol
        }

        // Replay transactions to rebuild holding
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal avgCost = BigDecimal.ZERO;

        for (SandboxTransaction t : remaining) {
            if (t.getType() == SandboxTransaction.TransactionType.BUY) {
                BigDecimal oldTotal = qty.multiply(avgCost);
                BigDecimal tradeValue = t.getQuantity().multiply(t.getPrice());
                qty = qty.add(t.getQuantity());
                if (qty.compareTo(BigDecimal.ZERO) > 0) {
                    avgCost = oldTotal.add(tradeValue).divide(qty, 4, RoundingMode.HALF_UP);
                }
            } else {
                qty = qty.subtract(t.getQuantity());
                // Average cost doesn't change on SELL
            }
        }

        if (qty.compareTo(BigDecimal.ZERO) > 0) {
            SandboxHolding holding = SandboxHolding.builder()
                    .portfolio(portfolio)
                    .symbol(symbol)
                    .quantity(qty)
                    .averageCost(avgCost)
                    .build();
            holdingRepository.save(holding);
        }
    }
}
