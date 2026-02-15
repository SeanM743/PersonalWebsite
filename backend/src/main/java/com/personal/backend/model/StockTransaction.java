package com.personal.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transactions", indexes = {
        @Index(name = "idx_txn_user_id", columnList = "userId"),
        @Index(name = "idx_txn_symbol", columnList = "symbol"),
        @Index(name = "idx_txn_date", columnList = "transactionDate")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 10)
    @NotBlank(message = "Stock symbol is required")
    @Pattern(regexp = "^[A-Z]{1,10}$", message = "Stock symbol must be 1-10 uppercase letters")
    private String symbol;

    @Column(nullable = false)
    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    private TransactionType type; // BUY or SELL

    @Column(nullable = false)
    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    @Column(nullable = false, precision = 19, scale = 8)
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.00000001", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Price per share is required")
    private BigDecimal pricePerShare;

    // Optional: Explicit total cost (usually qty * price, but fees might apply)
    @Column(precision = 19, scale = 4)
    private BigDecimal totalCost;

    // Optional: which account to debit/credit (null = default "Fidelity Cash")
    @Column
    private Long accountId;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TransactionType {
        BUY, SELL
    }
}
