package com.personal.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_balance_history", indexes = {
        @Index(name = "idx_hist_account_id", columnList = "accountId"),
        @Index(name = "idx_hist_date", columnList = "date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBalanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotNull
    private Long accountId;

    @Column(nullable = false)
    @NotNull
    private LocalDate date;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull
    private BigDecimal balance;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime recordedAt = LocalDateTime.now();
}
