package com.personal.backend.dto;

import com.personal.backend.model.SandboxTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxPortfolioDetailDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance; // Cash
    private BigDecimal holdingsValue;  // Sum of market values of holdings
    private BigDecimal totalValue;     // Cash + Holdings Value
    private BigDecimal totalGainLoss;
    private BigDecimal totalGainLossPercentage;
    private LocalDateTime createdAt;
    
    private List<SandboxHoldingDTO> holdings;
    private List<SandboxTransaction> recentTransactions;
}
