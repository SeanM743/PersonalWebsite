package com.personal.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "paper_transactions", indexes = {
        @Index(name = "idx_paper_user_id", columnList = "userId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotNull
    private Long userId;

    @Column(nullable = false, length = 10)
    @NotBlank
    private String symbol;

    @Column(nullable = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    private StockTransaction.TransactionType type; // Reuse existing Enum

    @Column(nullable = false, precision = 19, scale = 8)
    @NotNull
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull
    private BigDecimal pricePerShare;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime transactionDate = LocalDateTime.now();
    
    @Column(length = 500)
    private String notes;
}
