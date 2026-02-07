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
@Table(name = "watchlist_items", indexes = {
        @Index(name = "idx_watchlist_user_id", columnList = "userId"),
        @Index(name = "idx_watchlist_symbol", columnList = "symbol")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 10)
    @NotBlank
    @Pattern(regexp = "^[A-Z]{1,10}$", message = "Stock symbol must be 1-10 uppercase letters")
    private String symbol;

    @Column(precision = 19, scale = 4)
    private BigDecimal targetPrice; // Alert/Target price

    @Column(length = 200)
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();
}
