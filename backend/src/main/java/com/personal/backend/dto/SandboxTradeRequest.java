package com.personal.backend.dto;

import com.personal.backend.model.SandboxTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxTradeRequest {
    private String symbol;
    private SandboxTransaction.TransactionType type;
    private BigDecimal quantity;
    private BigDecimal dollarAmount; // Optional: if provided, quantity is calculated as dollarAmount / price
    private BigDecimal price; // Optional: user can override market price
    private LocalDate date;
}
