package com.personal.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_account_type", columnList = "type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Account name is required")
    private String name; // e.g., "Roth IRA", "Stock Portfolio"

    @Column(nullable = false)
    @NotNull(message = "Account type is required")
    @Enumerated(EnumType.STRING)
    private AccountType type;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Balance is required")
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isManual = true; // False if calculated from stock holdings

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum AccountType {
        STOCK_PORTFOLIO, // Dynamic, based on holdings
        CASH,
        RETIREMENT,      // Roth, 401k
        EDUCATION,       // 529s
        OTHER
    }
}
