package com.personal.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_transactions", indexes = {
        @Index(name = "idx_acct_txn_account_id", columnList = "accountId"),
        @Index(name = "idx_acct_txn_date", columnList = "transactionDate")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal oldBalance;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal newBalance;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(length = 500)
    private String description;

    // Links back to the stock transaction that caused this cash movement
    @Column
    private Long relatedStockTransactionId;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TransactionType {
        DEBIT,
        CREDIT
    }
}
